import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { RouterController } from 'router-controller';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/tooltip';
import '@vaadin/checkbox';
import '@vaadin/number-field';
import '@vaadin/integer-field';
import '@vaadin/text-field';
import '@vaadin/select';
import '@vaadin/details';
import '@vaadin/combo-box';
import '@qomponent/qui-badge';
import { notifier } from 'notifier';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { gridRowDetailsRenderer } from '@vaadin/grid/lit.js';
import { observeState } from 'lit-element-state';
import { connectionState } from 'connection-state';
import { devuiState } from 'devui-state';

/**
 * This component allows users to change the configuration
 */
export class QwcConfiguration extends observeState(LitElement) {

    jsonRpc = new JsonRpc(this);
    routerController = new RouterController(this);

    static styles = css`
      .conf {
        height: 100%;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }

      .confTopBar {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      vaadin-grid {
        height: 100%;
      }

      vaadin-grid-cell-content {
        vertical-align: top;
        width: 100%;
      }

      .description {
        padding: 1em;
      }

      .input-column {
        width: 100%;
        vertical-align: top;
        padding: unset;
      }

      .full-height {
        height: 100%;
      }

      .save-button {
        background-color: transparent;
        cursor: pointer;
        color: var(--lumo-primary-color);
      }

      .lock-icon {
        color: var(--lumo-contrast-60pct);
        font-size: small;
      }
      .unlock-icon {
        color: var(--lumo-contrast-60pct);
        font-size: small;
      }

      .disabledDatatable {
        pointer-events: none;
        opacity: 0.4;
      }
      .config-source-dropdown {
        padding-left: 5px;
        width: 300px;
        margin-right: 2px;
      }
    `;

    static properties = {
        _filtered: {state: true, type: Array}, // Filter the visible configuration
        _visibleConfiguration: {state: true, type: Array}, // Either all or just user's configuration
        _allConfiguration: {state: true, type: Array},
        _detailsOpenedItem: {state: true, type: Array},
        _busy: {state: true},
        _showOnlyConfigSource: {state: true},
        _searchTerm: {state: true},
        _configSourceSet: {state: true}
    };

    constructor() {
        super();
        this._configSourceSet = new Map();
        this._detailsOpenedItem = [];
        this._busy = null;

        this._showOnlyConfigSource = null;
        this._searchTerm = '';
        this._applicationProperties = {
            name: 'application.properties',
            display: 'application.properties',
            overwritableInApplicationProperties: true
        }
    }

    connectedCallback() {
        super.connectedCallback();
        this._filteredValue = this.routerController.getQueryParameter("filter");

        if(this._filteredValue){
            this._filteredValue = this._filteredValue.replaceAll(",", " OR ");
        }
        this.jsonRpc
            .getFullPropertyConfiguration()
            .then(e => e.result)
            .then(
                result => result
                    .map(configItem => this._prepareConfiguration(configItem))
                    .filter(configItem => !configItem.disabled)
            )
            .then(result => {
                this._allConfiguration = result;
                this._visibleConfiguration = result;
                this._filtered = result;
            });
    }

    _prepareConfiguration(configItem) {
        configItem.configDescription.configSourceObject =
            this._findOrCreateConfigSourceObject(configItem.configDescription.configValue);
        // if the configuration is explicitely set in application.properties,
        // set it as source (no matter which config source was calculated by SmallRye Config)
        if(configItem.sourceValue
                && configItem.configDescription.configSourceObject !== this._applicationProperties) {
            configItem.configDescription.configSourceObject = this._applicationProperties;
        }
        // properties beginning with % are profile-specific
        const name = configItem.configDescription.name;
        if(name.startsWith('%') && name.includes('.')) {
            configItem.profile = name.substring(1, name.indexOf('.'));
            configItem.configDescription.name = name.substring(configItem.profile.length+2);
            configItem.disabled = true;
        }
        return configItem;
    }

    _findOrCreateConfigSourceObject(configValue) {
        let configSourceName = this._getConfigSourceName(configValue);
        if(configSourceName){
            if(!this._configSourceSet.has(configSourceName)){
                const configSourceObject = this._createConfigSourceObject(configValue);
                this._configSourceSet.set(configSourceName, configSourceObject);
                return configSourceObject;
            } else {
                return this._configSourceSet.get(configSourceName);
            }
        }
    }

    get _document() {
        return document
            .querySelector('qwc-configuration')
            .shadowRoot;
    }

    _doBusy(promiseSupplier) {
        this._busy = true;
        promiseSupplier()
            .finally(() => this._busy = null);
    }

    _getConfigSourceName(configValue){
        if(configValue.sourceName){
            return configValue.configSourceName;
        }
        return null;
    }

    _isApplicationProperties(configSource) {
        const configSourceName = this._getConfigSourceName(configSource);
        return configSourceName?.startsWith("PropertiesConfigSource[source")
            && configSourceName?.endsWith("/application.properties]");
    }

    _createConfigSourceObject(configSource){

        const name = this._getConfigSourceName(configSource);
        const applicationProperties = this._isApplicationProperties(configSource);

        if(applicationProperties) {
            this._applicationProperties.name = name;
            this._applicationProperties.position = configSource.configSourcePosition;
            this._applicationProperties.ordinal = configSource.configSourceOrdinal;
            return this._applicationProperties;
        }

        let displayName = name;
        let overwritableInApplicationProperties = true;
        switch (name) {
            case 'SysPropConfigSource':
                displayName = 'System Properties';
                overwritableInApplicationProperties = false;
                break;
            case 'EnvConfigSource':
                displayName = 'Environment Variables';
                overwritableInApplicationProperties = false;
                break;
            case 'DefaultValuesConfigSource':
                displayName = '(defaults)';
                break;
        }

        return {
            name,
            display: displayName,
            overwritableInApplicationProperties,
            position: configSource.configSourcePosition,
            ordinal:configSource.configSourceOrdinal
        };
    }

    render() {
        if (this._filtered) {
            return this._render();
        } else if(!connectionState.current.isConnected){
            return html`<span>Waiting for backend connection...</span>`;
        } else {
            return html`<span>Loading configuration properties...</span>`;
        }
    }

    _match(value, term) {
        if (! value) {
            return false;
        }
        if(term.includes(" OR ")){
            let terms = term.split(" OR ");
            for (let t of terms) {
                if(value.toLowerCase().includes(t.toLowerCase())){
                    return true;
                }
            }
            return false;
        }
        return value.toLowerCase().includes(term.toLowerCase());
    }

    _filterTextChanged(e) {
        this._searchTerm = (e.detail.value || '').trim();
        return this._filterGrid();
    }

    _filterGrid(){
        if (this._searchTerm === '') {
            this._filtered = this._visibleConfiguration;
            return;
        }

        this._filtered = this._visibleConfiguration.filter((prop) => {
           return  this._match(prop.configDescription.name, this._searchTerm) || this._match(prop.configDescription.description, this._searchTerm)
        });
    }

    _render() {
        return html`<div class="conf">
                <div class="confTopBar">
                    <vaadin-combo-box class="config-source-dropdown"
                                      @change="${this._toggleFilterByConfigSource}"
                                      placeholder="Filter by config sources"
                                      item-label-path="display"
                                      item-value-path="name"
                                      .items="${Array.from(
                                                    this._configSourceSet
                                                        .values())
                                                        .sort((a, b) => -(a.ordinal === b.ordinal ? 1 : a.ordinal -b.ordinal)
                                                        )}"
                                      clear-button-visible
                    ></vaadin-combo-box>
                    <vaadin-text-field
                            placeholder="Filter by name"
                            value="${this._filteredValue}"
                            style="flex: 1;"
                            @value-changed="${this._filterTextChanged}">
                        <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
                        <qui-badge slot="suffix"><span>${this._filtered.length}</span></qui-badge>
                    </vaadin-text-field>
                </div>
                ${this._renderGrid()}
                </div>`;
    }

    _toggleFilterByConfigSource(event){
        if(event.target.value){
            this._showOnlyConfigSource = event.target.value;
            this._visibleConfiguration = this._allConfiguration
                .filter(prop => prop.configDescription?.configSourceObject?.name === this._showOnlyConfigSource);
        } else {
            this._showOnlyConfigSource = null;
            this._visibleConfiguration = this._allConfiguration;
        }
        return this._filterGrid();
    }

    get _gridComponent() {
        return this._document.querySelector('#configuration-grid');
    }

    _renderGrid(){
        return html`<vaadin-grid
                        id="configuration-grid"
                        .items="${this._filtered}" 
                        style="width: 100%;" 
                        class="${this._busy ? 'disabledDatatable' : 'datatable'}" 
                        theme="row-stripes"
                        .detailsOpenedItems="${this._detailsOpenedItem}"
                        @active-item-changed="${(event) => {
                            const prop = event.detail.value;
                            this._detailsOpenedItem = prop ? [prop] : [];
                        }}"
                        ${gridRowDetailsRenderer(this._descriptionRenderer, [])}
                    >

                        <vaadin-grid-sort-column auto-width class="cell" flex-grow="0" path="configPhase" header='Phase'
                                            ${columnBodyRenderer(this._lockRenderer, [])}>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column auto-width class="cell" flex-grow="0" path="configValue" header='Source'
                                     ${columnBodyRenderer(this._configSourceRenderer, [])}>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-sort-column width="45%" resizable flex-grow="0"
                                            header="Name"
                                            path="name"
                                            class="cell"
                                            ${columnBodyRenderer(this._nameRenderer, [])}>
                        </vaadin-grid-sort-column>

                        <vaadin-grid-column auto-width resizable
                                            class="cell"
                                            header="Value"
                                            ${columnBodyRenderer(this._valueRenderer, [])}>
                        </vaadin-grid-column>
                    </vaadin-grid>`;
    }

    _lockRenderer(prop) {
        if (prop.configDescription.configPhase === "BUILD_AND_RUN_TIME_FIXED"
            || prop.configDescription.configPhase === "BUILD_TIME") {
            return html`
                <vaadin-icon theme="small" class="lock-icon" id="icon-lock-${prop.configDescription.name}" icon="font-awesome-solid:lock"></vaadin-icon>
                <vaadin-tooltip for="icon-lock-${prop.configDescription.name}" text="Fixed at build time (not overridable at runtime)"
                                position="top-start"></vaadin-tooltip>
            `
        } else {
            return html``;
        }
    }

    _configSourceRenderer(prop) {
        if(prop.configDescription.configSourceObject) {
            return html`
                <qui-badge small title="${prop.configDescription.configSourceObject.name}">
                    ${prop.configDescription.configSourceObject.display}
                </qui-badge>
            `;
        } else {
            return html``;
        }
    }

    _nameRenderer(prop) {
        let devservice = "";
        let wildcard = "";
        let profile = "";
        if (prop.configDescription.autoFromDevServices) {
            devservice = html`
                <vaadin-icon id="icon-dev-${prop.configDescription.name}" icon="font-awesome-solid:wand-magic-sparkles"></vaadin-icon>
                <vaadin-tooltip for="icon-dev-${prop.configDescription.name}" text="Automatically set by Dev Services"
                                position="top-start"></vaadin-tooltip>
            `;
        }

        if (prop.configDescription.wildcardEntry) {
            wildcard = html`
                <vaadin-icon id="icon-wc-${prop.configDescription.name}" icon="font-awesome-solid:plus"></vaadin-icon>
                <vaadin-tooltip for="icon-wc-${prop.configDescription.name}" text="This will add a new named config group"
                                position="top-start"></vaadin-tooltip>
            `;
        }

        if (prop.profile) {
            profile = html`
                <qui-badge small 
                           background="${prop.disabled ? 'lightgray' : 'purple'}" 
                           color="${prop.disabled ? 'darkgray' : 'white'}">
                    ${prop.profile}
                </qui-badge>
            `;
        }

        let result = html`
            ${profile}<code>${prop.configDescription.name}</code>${devservice}${wildcard}`;
        if(prop.disabled) {
            result = html`
                <span style="text-decoration: line-through">${result}</span>
                
            `;
        }
        return result;
    }

    _renderInputForBoolean(propertyContext) {
        return html`
            ${this._renderCalculatedValueWarning(propertyContext)}
            <vaadin-checkbox
                    theme="small"
                    @change="${!propertyContext.editable ? null : event => this._checkedChanged(event, propertyContext.property)}"
                    data-property-input="${propertyContext.property.configDescription.name}"
                    data-property-type="boolean"
                    ?readonly="${!propertyContext.editable}"
                    value="true"
                    .checked=${['true', 'enabled', 'on'].indexOf(propertyContext.value) > -1}>
                ${this._renderInputTooltip(propertyContext)}
            </vaadin-checkbox>
            ${this._renderOverwriteButton(propertyContext)}
        `;
    }

    _renderInputForEnum(propertyContext) {
        return html`
            <vaadin-select class="input-column"
                           data-property-input="${propertyContext.property.configDescription.name}"
                           data-property-type="string"
                           theme="small"
                           .items="${propertyContext
                                   .property
                                   .configDescription
                                   .allowedValues
                                   ?.map(value => {
                                        return {
                                           value: value, 
                                           label: value
                                       }
                                   }) ?? [] }"
                           .value="${propertyContext.value}"
                           ?readonly="${!propertyContext.editable}"
                           @change="${!propertyContext.editable ? null : this._selectChanged}">
                ${this._renderCalculatedValueWarning(propertyContext)}
                ${this._renderInputTooltip(propertyContext)}
                ${this._renderOverwriteButton(propertyContext)}
            </vaadin-select>
        `;
    }

    _renderInputForInteger(propertyContext) {
        return html`
            <vaadin-integer-field class="input-column"
                                  placeholder="${propertyContext.property.configDescription}"
                                  .value="${propertyContext.value}"
                                  theme="small"
                                  data-property-input="${propertyContext.property.configDescription.name}"
                                  data-property-type="number"
                                  ?readonly="${!propertyContext.editable}"
                                  @keydown="${!propertyContext.editable ? null : this._keydown}">
                ${this._renderCalculatedValueWarning(propertyContext)}
                ${this._renderInputTooltip(propertyContext)}
                ${this._renderSaveButton(propertyContext)}
            </vaadin-integer-field>
        `;
    }

    _renderInputForDouble(propertyContext) {
        return html`
            <vaadin-number-field class="input-column"
                                 placeholder="${propertyContext.property.configDescription}"
                                 .value="${propertyContext.value}"
                                 theme="small"
                                 data-property-input="${propertyContext.property.configDescription.name}"
                                 data-property-type="number"
                                 ?readonly="${!propertyContext.editable}"
                                 @keydown="${!propertyContext.editable ? null : this._keydown}">
                ${this._renderCalculatedValueWarning(propertyContext)}
                ${this._renderInputTooltip(propertyContext)}
                ${this._renderSaveButton(propertyContext)}
            </vaadin-number-field>
        `;
    }

    _renderInputForText(propertyContext) {
        return html`
            <vaadin-text-field class="input-column"
                               placeholder="${propertyContext.property.configDescription}"
                               .value="${propertyContext.value}"
                               theme="small"
                               data-property-input="${propertyContext.property.configDescription.name}"
                               data-property-type="string"
                               ?readonly="${!propertyContext.editable}"
                               @keydown="${!propertyContext.editable ? null : this._keydown}">
                ${this._renderCalculatedValueWarning(propertyContext)}
                ${this._renderInputTooltip(propertyContext)}
                ${this._renderSaveButton(propertyContext)}
            </vaadin-text-field>
        `;
    }

    _renderInputTooltip(propertyContext, componentId) {
        const defaultValueText =
            propertyContext.property.configDescription.defaultValue
                ? `Default value: ${propertyContext.property.configDescription.defaultValue}`
                : 'No default value';
        let warningText = '';
        if(propertyContext.editable
                && propertyContext.value !== propertyContext.property.sourceValue) {
            warningText = `Raw value differs from calculated value: "${propertyContext.property.sourceValue}"! \n`;
        }
        return html`<vaadin-tooltip slot="tooltip" text="${warningText}${defaultValueText}"></vaadin-tooltip>`;
    }

    _renderCalculatedValueWarning(propertyContext) {
        if(propertyContext.editable
            && propertyContext.value !== propertyContext.property.sourceValue) {
            return html`
                <vaadin-icon id="warn-value-${propertyContext.property.name}"
                             slot="prefix"
                             icon="font-awesome-solid:triangle-exclamation"
                             class="text-warn"></vaadin-icon>
                <vaadin-tooltip for="warn-value-${propertyContext.property.name}"
                                text="Raw value differs from calculated value: '${propertyContext.property.sourceValue}'"></vaadin-tooltip>
            `;
        }
    }

    _renderSaveButton(propertyContext) {
        if(propertyContext.editable) {
            return html`
                <vaadin-icon slot="suffix"
                             icon="font-awesome-solid:floppy-disk"
                             class="save-button"
                             @click="${event => this._saveClicked(event, propertyContext.property)}"></vaadin-icon>
            `;
        } else {
            return this._renderOverwriteButton(propertyContext);
        }
    }

    _renderOverwriteButton(propertyContext) {
        if(propertyContext.overwritable && this._applicationProperties) {
            return html`
                <vaadin-icon slot="suffix"
                             icon="font-awesome-solid:pen-to-square"
                             class="save-button"
                             id="overwrite-button-${propertyContext.property.configDescription.name}"
                             @click="${event => this._overwriteClicked(event, propertyContext.property)}"></vaadin-icon>
                <vaadin-tooltip for="overwrite-button-${propertyContext.property.configDescription.name}" 
                                text="Overwrite in application.properties"
                                position="top-start"></vaadin-tooltip>
            `;
        } else {
            return html``;
        }
    }

    _isOverwritable(configDescription) {
        return configDescription.configSourceObject !== this._applicationProperties
            && !configDescription.name.includes('*')
            && (configDescription.configSourceObject?.overwritableInApplicationProperties ?? true)
    }

    _valueRenderer(prop) {

        const propertyContext = {
            property: prop,
            value: prop.configDescription.configValue?.value ?? prop.configDescription.defaultValue,
            editable: prop.configDescription.configSourceObject === this._applicationProperties,
            overwritable: this._isOverwritable(prop.configDescription),
        }

        if (prop.configDescription.wildcardEntry) {
            // TODO
        } else {
            switch (prop.configDescription.typeName) {
                case 'java.lang.Boolean':
                    return this._renderInputForBoolean(propertyContext);
                case 'java.lang.Enum':
                case 'java.util.logging.Level':
                    return this._renderInputForEnum(propertyContext);
                case 'java.lang.Byte':
                case 'java.lang.Short':
                case 'java.lang.Integer':
                case 'java.lang.Long':
                    return this._renderInputForInteger(propertyContext);
                case 'java.lang.Number':
                case 'java.lang.Float':
                case 'java.lang.Double':
                    return this._renderInputForDouble(propertyContext);
                default:
                    return this._renderInputForText(propertyContext);
            }
        }
    }

    _descriptionRenderer(prop) {
        let val = prop.configDescription.name;
        let res = "";
        for (let i = 0; i < val.length; i++) {
            let c = val.charAt(i);
            if ('a' <= c && c <= 'z' || 'A' <= c && c <= 'Z' || '0' <= c && c <= '9') {
                res = res + c;
            } else {
                res = res + '_';
                if (c === '"'  && i + 1 === val.length) {
                    res = res + '_';
                }
            }
        }
        res = res.toUpperCase();

        let def = "<strong>Default value: </strong> None";
        if (prop.configDescription.defaultValue) {
            def = "<strong>Default value: </strong>" + prop.configDescription.defaultValue;
        }
        let configSourceName = "Unknown";
        if(prop.configDescription.configValue.sourceName){
            configSourceName = prop.configDescription.configValue.sourceName;
        }
        let src = "<strong>Config source: </strong> " + configSourceName;
        return html`<div class="description">
                        <p>${unsafeHTML(prop.configDescription.description)}</p>
                        <div>
                            <span><strong>Environment variable: </strong></span><code>${res}</code><br/>
                            <span>${unsafeHTML(def)}</span><br/>
                            <span>${unsafeHTML(src)}</span>
                        </div>
                    </div>`;
    }

    _keydown(event){
        if (event.key === 'Enter' || event.keyCode === 13) {
            let property = this._getPropertyOnInputField(event.target);
            this._doBusy(() => this._updateProperty(property, event.target.value));
        }
    }

    _selectChanged(event){
        let property = this._getPropertyOnInputField(event.target);
        this._doBusy(() => this._updateProperty(property, event.target.value));
    }

    _getPropertyOnInputField(input) {
        if(!input) {
            throw "Input does not have a data attribute for the property";
        }
        const result = input.dataset.propertyInput;
        return result
                ? this._allConfiguration.find(property => property.configDescription.name === result)
                : this._getPropertyOnInputField(input.parentElement);
    }

    _getInputFieldForProperty(property) {
        return this._document
            .querySelector(`[data-property-input='${property.configDescription.name}']`);
    }

    _saveClicked(event, property){
        event.preventDefault();
        let newValue = this._getInputFieldForProperty(property).value;
        this._doBusy(
            () => this
                ._updateProperty(property, newValue)
                .then(() => this._gridComponent.requestContentUpdate())
        );
    }

    _overwriteClicked(event, property){
        event.preventDefault();
        const inputField = this._getInputFieldForProperty(property);
        const newValue = inputField.dataset.propertyType === "boolean"
            ? "" + inputField.checked
            : inputField.value;
        this._doBusy(
            () =>this
                ._updateProperty(property, newValue)
                .then(property => {
                    property.configDescription.configSourceObject = this._applicationProperties;
                    return property;
                })
                .then(() => this._gridComponent.requestContentUpdate())
        );
    }

    _checkedChanged(event, property) {
        event.preventDefault();
        const newValue = event.target.checked;
        this._doBusy(() => this._updateProperty(property, newValue.toString()));
    }

    _updateProperty(property, newValue){
        const propertyName = (property.profile ? `%${property.profile}.` : '' ) + property.configDescription.name;
        return this.jsonRpc.updateProperty({
                'name': propertyName,
                'value': newValue
            })
            .then(() => {
                property.configDescription.configValue.value = newValue;
                property.sourceValue = newValue;
                fetch(devuiState.applicationInfo.contextRoot);
                notifier.showInfoMessage(`Property <code>${propertyName}</code> updated`);
                return property;
            });
    }
}

customElements.define('qwc-configuration', QwcConfiguration);
