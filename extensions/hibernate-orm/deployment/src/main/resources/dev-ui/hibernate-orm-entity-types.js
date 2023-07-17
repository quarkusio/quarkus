import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';

export class HibernateOrmEntityTypesComponent extends LitElement {

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