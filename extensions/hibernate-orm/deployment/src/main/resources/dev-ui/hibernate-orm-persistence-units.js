import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { notifier } from 'notifier';

export class HibernateOrmPersistenceUnitsComponent extends LitElement {

    static styles = css`
        .full-height {
          height: 100%;
        }
        .ddl-script {
            padding: 5px;
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

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getInfo().then(response => {
            this._persistenceUnits = response.result.persistenceUnits;
        });
    }

    render() {
        if (this._persistenceUnits) {
            return this._renderAllPUs();
        } else {
            return html`<span>Loading...</span>`;
        }
    }

    _renderAllPUs() {
        return this._persistenceUnits.length == 0
            ? html`<p>No persistence units were found.</p>`
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
                    <pre class="ddl-script">${pu.createDDL}</pre>
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
                    <pre class="ddl-script">${pu.updateDDL}</pre>
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
                    <pre class="ddl-script">${pu.dropDDL}</pre>
                </vaadin-details>`;
    }

    _copyToClipboard(event, what) {
        event.stopPropagation();
        var text = event.target.closest("vaadin-details").querySelector(".ddl-script").textContent;
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
