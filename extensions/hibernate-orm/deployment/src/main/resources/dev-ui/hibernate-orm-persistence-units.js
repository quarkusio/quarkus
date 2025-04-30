import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/progress-bar';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { notifier } from 'notifier';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@quarkus-webcomponents/codeblock';

export class HibernateOrmPersistenceUnitsComponent extends observeState(QwcHotReloadElement) {

    static styles = css`
        :host {
            display: flex;
            padding-left: 10px;
            padding-right: 10px;
        }
        .full-height {
            height: 100%;
            width: 100%;
        }
        a.script-heading {
            display: block;
            float:left;
            width: 90%;
            text-decoration: none;
        }
    `;

    jsonRpc = new JsonRpc(this);

    static properties = {
        _persistenceUnits: {state: true, type: Array}
    }

    constructor() {
        super();
        this._persistenceUnits = [];
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload(){
        this.jsonRpc.getInfo().then(response => {
            this._persistenceUnits = response.result.persistenceUnits;
        }).catch(error => {
            console.error("Failed to fetch persistence units:", error);
            this._persistenceUnits = []; 
            notifier.showErrorMessage("Failed to fetch persistence units: " + error, "bottom-start", 30);
        });
    }

    render() {
        if (this._persistenceUnits) {
            return this._renderAllPUs();
        } else {
            return html`<div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                            <div>Fetching persistence units...</div>
                            <vaadin-progress-bar indeterminate></vaadin-progress-bar>
                        </div>`;
        }
    }

    _renderAllPUs() {
        return this._persistenceUnits.length == 0
            ? html`<p>No persistence units were found. 
                        <vaadin-button @click="${this.hotReload}" theme="small">Check again</vaadin-button>
                    </p>`
            : html`
                    <vaadin-tabsheet class="full-height">
                        <span slot="prefix">Persistence Unit</span>
                        <vaadin-tabs slot="tabs">
                            ${this._persistenceUnits.map((pu) => html`
                                <vaadin-tab id="pu-${pu.name}-persistence-unit">
                                    <span>${pu.name}</span>
                                </vaadin-tab>
                                `)}
                        </vaadin-tabs>

                        ${this._persistenceUnits.map((pu) => html`
                            <div class="full-height" tab="pu-${pu.name}-persistence-unit">
                                ${this._renderPersistenceUnit(pu)}
                            </div>
                            `)}
                    </vaadin-tabsheet>`;
    }

    _renderPersistenceUnit(pu) {
        return html`
                <vaadin-details>
                    <vaadin-details-summary slot="summary" theme="filled">
                        <vaadin-horizontal-layout
                                theme="spacing"
                                style="align-items: center;">
                            <span>Create Script</span>
                            <vaadin-button @click="${(e) => this._copyToClipboard(e, 'Create Script')}"
                                    theme="small">
                                <vaadin-icon icon="font-awesome-solid:clipboard"></vaadin-icon>
                                Copy
                            </vaadin-button>
                        </vaadin-horizontal-layout>
                    </vaadin-details-summary>
                    <qui-code-block
                        mode="sql"
                        content="${pu.createDDL}"
                        theme="${themeState.theme.name}">
                    </qui-code-block>
                </vaadin-details>
                <vaadin-details>
                    <vaadin-details-summary slot="summary" theme="filled">
                        <vaadin-horizontal-layout
                                theme="spacing"
                                style="align-items: center;">
                            <span>Update Script</span>
                            <vaadin-button @click="${(e) => this._copyToClipboard(e, 'Update Script')}"
                                    theme="small">
                                <vaadin-icon icon="font-awesome-solid:clipboard"></vaadin-icon>
                                Copy
                            </vaadin-button>
                        </vaadin-horizontal-layout>
                    </vaadin-details-summary>
                    <qui-code-block
                        mode="sql"
                        content="${pu.updateDDL}"
                        theme="${themeState.theme.name}">
                    </qui-code-block>
                </vaadin-details>
                <vaadin-details>
                    <vaadin-details-summary slot="summary" theme="filled">
                        <vaadin-horizontal-layout
                                theme="spacing"
                                style="align-items: center;">
                            <span>Drop Script</span>
                            <vaadin-button @click="${(e) => this._copyToClipboard(e, 'Drop Script')}"
                                    theme="small">
                                <vaadin-icon icon="font-awesome-solid:clipboard"></vaadin-icon>
                                Copy
                            </vaadin-button>
                        </vaadin-horizontal-layout>
                    </vaadin-details-summary>
                    <qui-code-block
                        mode="sql"
                        content="${pu.dropDDL}"
                        theme="${themeState.theme.name}">
                    </qui-code-block>
                </vaadin-details>`;
    }

    _copyToClipboard(event, what) {
        event.stopPropagation();
        var text = event.target.closest("vaadin-details").querySelector("qui-code-block").value;
        var listener = function(ev) {
            ev.clipboardData.setData("text/plain", text);
            ev.preventDefault();
        };
        document.addEventListener("copy", listener);
        document.execCommand("copy");
        document.removeEventListener("copy", listener);
        notifier.showInfoMessage('Copied "' + what + '" to clipboard.');
    }

}
customElements.define('hibernate-orm-persistence-units', HibernateOrmPersistenceUnitsComponent);
