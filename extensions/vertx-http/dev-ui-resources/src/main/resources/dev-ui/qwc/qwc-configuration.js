import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { RouterController } from 'router-controller';
import '@vaadin/grid';
import 'qui/qui-alert.js';
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
import { notifier } from 'notifier';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { gridRowDetailsRenderer } from '@vaadin/grid/lit.js';
import { observeState } from 'lit-element-state';
import { devuiState } from 'devui-state';
import { connectionState } from 'connection-state';
import 'qui-badge';

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
    `;

    static properties = {
        _filtered: {state: true, type: Array},
        _values: {state: true},
        _detailsOpenedItem: {state: true, type: Array},
        _busy: {state: true},
    };

    constructor() {
        super();

        this._filteredValue = this.routerController.getQueryParameter("filter");

        if(this._filteredValue){
            this._filteredValue = this._filteredValue.replaceAll(",", " OR ");
        }

        this._filtered = devuiState.allConfiguration;
        this.jsonRpc.getAllValues().then(e => {
            this._values = e.result;
        });
        this._detailsOpenedItem = [];
        this._busy = null;
    }

    render() {
        if (this._filtered && this._values) {
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
        const searchTerm = (e.detail.value || '').trim();
        if (searchTerm === '') {
            this._filtered = devuiState.allConfiguration;
            return;
        }

        this._filtered = devuiState.allConfiguration.filter((prop) => {
           return  this._match(prop.name, searchTerm) || this._match(prop.description, searchTerm)
        });
    }

    _render() {
        return html`<div class="conf">
                <vaadin-text-field
                        placeholder="Filter"
                        value="${this._filteredValue}"
                        style="width: 100%;"
                        @value-changed="${(e) => this._filterTextChanged(e)}">
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
                    <qui-badge slot="suffix"><span>${this._filtered.length}</span></qui-badge>
                </vaadin-text-field>
                ${this._renderGrid()}
                </div>`;
    }

    _renderGrid(){
        if(this._busy){
            return html`${this._renderStyledGrid("disabledDatatable")}`;
        }else{
            return html`${this._renderStyledGrid("datatable")}`;
        }
    }

    _renderStyledGrid(className){
        return html`<vaadin-grid .items="${this._filtered}" style="width: 100%;" class="${className}" theme="row-stripes"
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
        if (prop.configPhase === "BUILD_AND_RUN_TIME_FIXED" || prop.configPhase === "BUILD_TIME") {
            return html`
                <vaadin-icon theme="small" class="lock-icon" id="icon-lock-${prop.name}" icon="font-awesome-solid:lock"></vaadin-icon>
                <vaadin-tooltip for="icon-lock-${prop.name}" text="Fixed at build time (not overridable at runtime)"
                                position="top-start"></vaadin-tooltip>
            `
        }
    }

    _nameRenderer(prop) {
        let devservice = "";
        let wildcard = "";
        if (prop.autoFromDevServices) {
            devservice = html`
                <vaadin-icon id="icon-dev-${prop.name}" icon="font-awesome-solid:magic"></vaadin-icon>
                <vaadin-tooltip for="icon-dev-${prop.name}" text="Automatically set by Dev Services"
                                position="top-start"></vaadin-tooltip>
            `;
        }

        if (prop.wildcardEntry) {
            wildcard = html`
                <vaadin-icon id="icon-wc-${prop.name}" icon="font-awesome-solid:plus"></vaadin-icon>
                <vaadin-tooltip for="icon-wc-${prop.name}" text="This will add a new named config group"
                                position="top-start"></vaadin-tooltip>
            `;
        }

        return html`
            <code>${prop.name}</code>${devservice}${wildcard}`;
    }

    _valueRenderer(prop) {
        let def = '';
        if (prop.defaultValue) {
            def = "Default value: " + prop.defaultValue;
        } else {
            def = "No default value";
        }

        let actualValue = this._values[prop.name];
        if (!actualValue) {
            actualValue = prop.defaultValue;
        }

        if (prop.wildcardEntry) {
            // TODO
        } else if (prop.typeName === "java.lang.Boolean") {
            let isChecked = (actualValue === 'true');
            return html`
                <vaadin-checkbox theme="small"
                                @change="${(event) => {
                                    this._checkedChanged(prop, event, event.target.checked);
                                }}"
                                .checked=${isChecked}>
                    <vaadin-tooltip slot="tooltip" text="${def}"></vaadin-tooltip>
                </vaadin-checkbox>`;
        } else if (prop.typeName === "java.lang.Integer" || prop.typeName === "java.lang.Long") {
            return html`
                <vaadin-integer-field class="input-column"
                                    placeholder="${prop.defaultValue}"
                                    value="${actualValue}"
                                    theme="small"
                                    id="input-${prop.name}"
                                    @keydown="${this._keydown}">
                    <vaadin-tooltip slot="tooltip" text="${def}"></vaadin-tooltip>
                    <vaadin-icon slot="suffix" icon="font-awesome-solid:floppy-disk" class="save-button"
                                id="save-button-${prop.name}"
                                @click="${this._saveClicked}"></vaadin-icon>
                </vaadin-integer-field>`;
        } else if (prop.typeName === "java.lang.Float" || prop.typeName === "java.lang.Double") {
            return html`
                <vaadin-number-field class="input-column" 
                                    theme="small" 
                                    id="input-${prop.name}" 
                                    placeholder="${prop.defaultValue}" 
                                    value="${actualValue}" 
                                    @keydown="${this._keydown}">
                    <vaadin-tooltip slot="tooltip" text="${def}"></vaadin-tooltip>
                    <vaadin-icon slot="suffix" icon="font-awesome-solid:floppy-disk" class="save-button"
                                id="save-button-${prop.name}" @click="${this._saveClicked}"></vaadin-icon>
                </vaadin-number-field>`;
        } else if (prop.typeName === "java.lang.Enum" || prop.typeName === "java.util.logging.Level") {
            let items = [];
            let defaultValue = '';
            for (let idx in prop.allowedValues) {
                if (prop.allowedValues[idx] === actualValue) {
                    defaultValue = prop.allowedValues[idx];
                }
                items.push({
                    'label': prop.allowedValues[idx],
                    'value': prop.allowedValues[idx],
                });
            }
            if (! defaultValue) {
                defaultValue = prop.defaultValue;
            }
            return html`
                <vaadin-select class="input-column" 
                                id="select-${prop.name}" 
                                theme="small" 
                                .items="${items}" 
                                .value="${defaultValue}"
                                @change="${this._selectChanged}">
                            <vaadin-tooltip slot="tooltip" text="${def}"></vaadin-tooltip>
                
                </vaadin-select>
            `;
        } else {
            return html`
                <vaadin-text-field class="input-column" 
                                    theme="small" 
                                    value="${actualValue}"
                                    placeholder="${prop.defaultValue}" 
                                    id="input-${prop.name}" 
                                    @keydown="${this._keydown}">
                        <vaadin-tooltip slot="tooltip" text="${def}"></vaadin-tooltip>
                        <vaadin-icon slot="suffix" icon="font-awesome-solid:floppy-disk" class="save-button" 
                                    id="save-button-${prop.name}" @click="${this._saveClicked}"></vaadin-icon>
                    </vaadin-button>
                </vaadin-text-field>
            `;
        }
    }

    _descriptionRenderer(prop) {
        let val = prop.name;
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
        if (prop.defaultValue) {
            def = "<strong>Default value: </strong>" + prop.defaultValue;
        }
        let src = "<strong>Config source: </strong> " + prop.configValue.sourceName;
        return html`<div class="description">
                        <p>${unsafeHTML(prop.description)}</p>
                        <div>
                            <span><strong>Environment variable: </strong></span><code>${res}</code><br/>
                            <span>${unsafeHTML(def)}</span><br/>
                            <span>${unsafeHTML(src)}</span>
                        </div>
                    </div>`;
    }

    _keydown(event){
        if (event.key === 'Enter' || event.keyCode === 13) {
            let name = event.target.parentElement.id.replace("input-", "");
            this._updateProperty(name, event.target.value);
        }
    }

    _selectChanged(event){
        let name = event.target.id.replace("select-", "");
        this._updateProperty(name, event.target.value);
    }

    _saveClicked(event){
        event.preventDefault();
        let parent = event.target.parentElement;
        let name = parent.id.replace("input-", "");
        this._updateProperty(name, parent.value);
    }

    _checkedChanged(property, event, value) {
        event.preventDefault();
        this._updateProperty(property.name, value.toString());
    }

    _updateProperty(name, value){
        this._busy = true;
        this.jsonRpc.updateProperty({
            'name': name,
            'value': value
        }).then(e => {
            this._values[name] = value;
            notifier.showInfoMessage("Property <code>" + name + "</code> updated");
            this._busy = null;
        });
    }
}

customElements.define('qwc-configuration', QwcConfiguration);