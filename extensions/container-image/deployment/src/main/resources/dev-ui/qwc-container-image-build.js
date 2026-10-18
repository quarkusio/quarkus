import {LitElement, html, css, render} from 'lit';
import {JsonRpc} from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import {builderTypes} from 'build-time-data';
import '@vaadin/text-field';
import '@vaadin/text-area';
import '@vaadin/form-layout';
import '@vaadin/progress-bar';
import '@vaadin/checkbox';
import '@vaadin/select';
import '@vaadin/item';
import '@vaadin/list-box';
import { msg, updateWhenLocaleChanges } from 'localization';


export class QwcContainerImageBuild extends LitElement {

    jsonRpc = new JsonRpc(this);

    static properties = {
        builders: {type: Array},
        types: {type: Array},
        selected_builder: {type: String},
        selected_type: {type: String},
        build_in_progress: {state: true, type: Boolean},
        build_complete: {state: true, type: Boolean},
        build_error: {state: true, type: Boolean},
        result: {type: String}
    }

    static styles = css`
      :host {
        padding-left: 5px;
        padding-right: 5px;
        display: flex;
        flex-direction: column;
       }
      .report {
        margin-top: 1em;
      }
    `;

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this.build_in_progress = false;
        this.build_complete = false;
        this.build_error = false;
        this.result = "";
        
        this.builders = [];
        if(builderTypes){
            this.builders = builderTypes;
        }

        this.types = [];
        this.types.push({name: msg('Default', { id: 'quarkus-container-image-default' }), value: ""});
        this.types.push({name: msg('Jar', { id: 'quarkus-container-image-jar' }), value: "jar"});
        this.types.push({name: msg('Mutable Jar', { id: 'quarkus-container-image-mutable-jar' }), value: "mutable-jar"});
        this.types.push({name: msg('Native', { id: 'quarkus-container-image-native' }), value: "native"});
    }

    /**
     * Called when it needs to render the components
     * @returns {*}
     */
    render() {
        const _builders = [];
        let _defaultBuilder = "";
        if(this.builders){
            this.builders.map(item => _builders.push({'label': item, 'value': item}));
            _defaultBuilder = _builders[0].label;
        }
        
        const _types = [];
        this.types.map(item => _types.push({'label': item.name, 'value': item.value}));
        const _defaultType = "jar";

        const _builderPicker = html`
            <vaadin-select
                    label=${msg('Image Builder', { id: 'quarkus-container-image-image-builder' })}
                    .items="${_builders}"
                    .value="${_defaultBuilder}"
                    ?disabled="${_builders.length === 1 || this.build_in_progress}"
                    @value-changed="${e => this.selected_builder = e.target.value}"
            ></vaadin-select>`;

        let progress;
        if (this.build_in_progress) {
            progress = html`
                <div class="report">
                    <div>${msg('Generating container images...', { id: 'quarkus-container-image-generating' })}</div>
                    <vaadin-progress-bar indeterminate theme="contrast"></vaadin-progress-bar>
                </div>`;
        } else if (this.build_complete) {
            progress = html`
                <div class="report">
                    <div>${this.result}</div>
                    <vaadin-progress-bar value="1"
                                         theme="${this.build_error} ? 'error' : 'success'"></vaadin-progress-bar>
                </div>`;
        } else {
            progress = html`
                <div class="report"></div>`;
        }

        return html`
            <p>${msg('Select the type of build (jar, native...) and the container image builder.', { id: 'quarkus-container-image-select-build-type' })}</p>
            <vaadin-select
                    label=${msg('Build Type', { id: 'quarkus-container-image-build-type' })}
                    .items="${_types}"
                    .value="${_defaultType}"
                    ?disabled="${this.build_in_progress}"
                    @value-changed="${e => this.selected_type = e.target.value}"
            ></vaadin-select>
            ${_builderPicker}
            <vaadin-button @click="${this._build}" ?disabled="${this.build_in_progress}">${msg('Build Container', { id: 'quarkus-container-image-build-container' })}</vaadin-button>
            ${progress}
        `;
    }

    _build() {
        this.build_complete = false;
        this.build_in_progress = true;
        this.build_error = false;
        this.result = "";
        this.jsonRpc.build({'type': this.selected_type, 'builder': this.selected_builder})
            .then(jsonRpcResponse => {
                const msg = jsonRpcResponse.result;
                if (msg.includes("created.")) {
                    this.result = msg;
                    this.build_complete = true;
                    this.build_in_progress = false;
                } else {
                    this.build_complete = true;
                    this.build_in_progress = false;
                    this.build_error = true;
                }
            });
    }

}

customElements.define('qwc-container-image-build', QwcContainerImageBuild);
