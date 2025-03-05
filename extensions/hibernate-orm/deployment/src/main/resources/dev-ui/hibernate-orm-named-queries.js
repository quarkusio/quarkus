import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/progress-bar';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { notifier } from 'notifier';

export class HibernateOrmNamedQueriesComponent extends QwcHotReloadElement {

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
                                <vaadin-tab id="pu-${pu.name}-named-queries">
                                    <span>${pu.name}</span>
                                    <qui-badge small><span>${pu.namedQueries.length}</span></qui-badge>
                                </vaadin-tab>
                                `)}
                        </vaadin-tabs>

                        ${this._persistenceUnits.map((pu) => html`
                            <div class="full-height" tab="pu-${pu.name}-named-queries">
                                ${this._renderNamedQueriesTable(pu)}
                            </div>
                            `)}
                    </vaadin-tabsheet>`;
    }

    _renderNamedQueriesTable(pu) {
        if (pu.namedQueries.length == 0) {
            return html`<p>No named queries were found. <vaadin-button @click="${this.hotReload}" theme="small">Check again</vaadin-button></p>`
        }
        return html`
                <vaadin-grid .items="${pu.namedQueries}" class="datatable" theme="no-border row-stripes">
                    <vaadin-grid-column auto-width
                                        header="Name"
                                        path="name">
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header="Query"
                                        path="query">
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header="Lock Mode"
                                        path="lockMode">
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header="Cacheable"
                                        path="cacheable">
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header="Query Type"
                                        path="type">
                    </vaadin-grid-column>
                </vaadin-grid>`;
    }

}
customElements.define('hibernate-orm-named-queries', HibernateOrmNamedQueriesComponent);