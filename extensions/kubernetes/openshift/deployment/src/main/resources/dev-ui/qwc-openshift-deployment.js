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
import 'qui/qui-alert.js';


export class QwcOpenshiftDeployment extends LitElement {

    jsonRpc = new JsonRpc(this);

    static properties = {
        builders: {type: Array},
        types: {type: Array},
        expose: {type: String},
        untrusted: {type: String},
        selected_type: {type: String},
        build_in_progress: {state: true, type: Boolean},
        build_complete: {state: true, type: Boolean},
        build_error: {state: true, type: Boolean},
        result: {type: String}
    }

    static styles = css`
      .report {
        margin-top: 1em;
        width: 80%;
      }
    `;

    constructor() {
        super();

        this.expose = "true";
        this.untrusted = "false";

        this.build_in_progress = false;
        this.build_complete = false;
        this.build_error = false;
        this.result = "";

        this.builders = [];
        if (builderTypes) {
            this.builders = builderTypes.list;
        }

        this.types = [];
        this.types.push({name: "Default", value: ""});
        this.types.push({name: "Jar", value: "jar"});
        this.types.push({name: "Mutable Jar", value: "mutable-jar"});
        this.types.push({name: "Native", value: "native"});
    }

    /**
     * Called when it needs to render the components
     * @returns {*}
     */
    render() {
        const _builders = [];
        let _defaultBuilder = "";
        if (this.builders) {
            this.builders.map(item => _builders.push({'label': item, 'value': item}));
            _defaultBuilder = _builders[0].label;
        }

        const _types = [];
        this.types.map(item => _types.push({'label': item.name, 'value': item.value}));
        const _defaultType = "jar";

        let progress;
        if (this.build_in_progress) {
            progress = html`
                <div class="report">
                    <div>Deploying...</div>
                    <vaadin-progress-bar indeterminate theme="contrast"></vaadin-progress-bar>
                </div>`;
        } else if (this.build_complete) {
            progress = html`
                <div class="report">
                    ${this.result}
                    <vaadin-progress-bar value="1"
                                         theme="${this.build_error} ? 'error' : 'success'"></vaadin-progress-bar>
                </div>`;
        } else {
            progress = html`
                <div class="report"></div>`;
        }

        return html`
            <vaadin-select
                    label="Build Type"
                    .items="${_types}"
                    .value="${_defaultType}"
                    ?disabled="${this.build_in_progress}"
                    @value-changed="${e => this.selected_type = e.target.value}"
            ></vaadin-select>

            <div>
            <vaadin-checkbox
                    label="Expose route"
                    id="checkbox-expose"
                    checked
                    .value="${this.expose}"
                    ?disabled="${this.build_in_progress}"
            ></vaadin-checkbox>
            </div>

            <div>
                <vaadin-checkbox
                        id="checkbox-trust"
                        label="Accept untrusted certificates"
                        .value="${this.untrusted}"
                        ?disabled="${this.build_in_progress}"
                ></vaadin-checkbox>
            </div>

            <vaadin-button @click="${this._build}" ?disabled="${this.build_in_progress}">Deploy</vaadin-button>
            ${progress}
        `;
    }

    _build() {
        this.build_complete = false;
        this.build_in_progress = true;
        this.build_error = false;
        this.result = "";
        this.build_complete = false;
        this.build_in_progress = true;
        this.build_error = false;

        const trustCheckBox = this.renderRoot.getElementById("checkbox-trust");
        const exposeCheckBox = this.renderRoot.getElementById("checkbox-expose");

        this.jsonRpc.build({'type': this.selected_type, "expose": exposeCheckBox.checked, "untrusted": trustCheckBox.checked})
            .then(jsonRpcResponse => {
                const msg = jsonRpcResponse.result;
                if (msg.includes("deployed successfully")) {
                    this.build_complete = true;
                    this.build_in_progress = false;
                    this.result = html`
                        <qui-alert level="success">
                            ${msg}
                        </qui-alert>`;
                } else {
                    this.build_complete = true;
                    this.build_in_progress = false;
                    this.build_error = true;
                    this.result = html`<qui-alert level="error"> The deployment failed: ${msg}</qui-alert>`;
                }
            });
    }

}

customElements.define('qwc-openshift-deployment', QwcOpenshiftDeployment);
