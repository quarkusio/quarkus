import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';

export class HibernateOrmNamedQueriesComponent extends LitElement {

    static styles = css`
        .full-height {
          height: 100%;
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
            return html`<p>No named queries were found.</p>`
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