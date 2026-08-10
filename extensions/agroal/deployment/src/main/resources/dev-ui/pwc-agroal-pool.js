import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the Agroal JDBC connection pools.
 * Shows pool sizing and runtime metrics only - no SQL, schema or data access.
 */
export class PwcAgroalPool extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
        }
        .toolbar {
            display: flex;
            justify-content: flex-end;
            padding: 5px 10px;
        }
        .datatable {
            height: 100%;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _pools: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getPools().then(jsonRpcResponse => {
            this._pools = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._pools) {
            return html`<span class="empty">Loading connection pools...</span>`;
        }
        if (this._pools.length === 0) {
            return html`<span class="empty">No active datasources.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-grid .items="${this._pools}" class="datatable" theme="no-border row-stripes">
                <vaadin-grid-sort-column auto-width header="Datasource" path="name" frozen></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Active" path="active"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Available" path="available"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Max used" path="maxUsed"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Awaiting" path="awaiting"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Min size" path="minSize"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Max size" path="maxSize"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Acquired" path="acquireCount"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Created" path="creationCount"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Leaks" path="leakDetectionCount"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Blocking avg (ms)" path="blockingTimeAverageMs"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Blocking max (ms)" path="blockingTimeMaxMs"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }
}
customElements.define('pwc-agroal-pool', PwcAgroalPool);
