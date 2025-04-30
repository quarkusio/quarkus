import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/progress-bar';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { notifier } from 'notifier';

export class HibernateOrmEntityTypesComponent extends QwcHotReloadElement {

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
            ? html`<p>No persistence units were found. <vaadin-button @click="${this.hotReload}" theme="small">Check again</vaadin-button></p>`
            : html`
                    <vaadin-tabsheet class="full-height">
                        <span slot="prefix">Persistence Unit</span>
                        <vaadin-tabs slot="tabs">
                            ${this._persistenceUnits.map((pu) => html`
                                <vaadin-tab id="pu-${pu.name}-entity-types">
                                    <span>${pu.name}</span>
                                    <qui-badge small><span>${pu.managedEntities.length}</span></qui-badge>
                                </vaadin-tab>
                                `)}
                        </vaadin-tabs>

                        ${this._persistenceUnits.map((pu) => html`
                            <div class="full-height" tab="pu-${pu.name}-entity-types">
                                ${this._renderEntityTypesTable(pu)}
                            </div>
                            `)}
                    </vaadin-tabsheet>`;
    }

    _renderEntityTypesTable(pu) {
        if (pu.managedEntities.length == 0) {
            return html`<p>No managed entity types were found.</p>`
        }
        return html`
                <vaadin-grid .items="${pu.managedEntities}" class="datatable" theme="no-border row-stripes">
                    <vaadin-grid-column auto-width
                                        header="JPA entity name"
                                        path="name">
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header="Class name"
                                        path="className">
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header="Table name"
                                        path="tableName">
                    </vaadin-grid-column>
                </vaadin-grid>`;
    }

}
customElements.define('hibernate-orm-entity-types', HibernateOrmEntityTypesComponent);