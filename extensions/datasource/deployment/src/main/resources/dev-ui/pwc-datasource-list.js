import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the configured datasources.
 * Shows non-sensitive configuration only - no URL, username, password or
 * credentials, and no schema reset.
 */
export class PwcDatasourceList extends LitElement {

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
        _datasources: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getDatasources().then(jsonRpcResponse => {
            this._datasources = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._datasources) {
            return html`<span class="empty">Loading datasources...</span>`;
        }
        if (this._datasources.length === 0) {
            return html`<span class="empty">No datasources configured.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-grid .items="${this._datasources}" class="datatable" theme="no-border row-stripes">
                <vaadin-grid-sort-column auto-width header="Datasource" path="name" frozen></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Database kind" path="dbKind"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Version" path="dbVersion"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Active" path="active"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Health excluded" path="healthExcluded"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }
}
customElements.define('pwc-datasource-list', PwcDatasourceList);
