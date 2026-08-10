import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the Liquibase changeset status and history. For each
 * datasource it shows the applied and pending changesets. No update / rollback /
 * clear actions and no credentials are exposed.
 */
export class PwcLiquibaseChangesets extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            overflow: auto;
            gap: 20px;
            padding: 10px;
        }
        .toolbar {
            display: flex;
            justify-content: flex-end;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .datasource {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        .datasource-name {
            font-size: var(--lumo-font-size-l);
            font-weight: bold;
        }
        .error {
            color: var(--lumo-error-text-color);
            font-size: var(--lumo-font-size-s);
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
        this.jsonRpc.getChangeSets().then(jsonRpcResponse => {
            this._datasources = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._datasources) {
            return html`<span class="empty">Loading Liquibase changesets...</span>`;
        }
        if (this._datasources.length === 0) {
            return html`<span class="empty">No Liquibase datasources configured.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            ${this._datasources.map(ds => this._renderDatasource(ds))}`;
    }

    _renderDatasource(ds) {
        return html`
            <div class="datasource">
                <span class="datasource-name">${ds.name}</span>
                ${ds.error
                    ? html`<span class="error">${ds.error}</span>`
                    : this._renderChangeSets(ds.changeSets)}
            </div>`;
    }

    _renderChangeSets(changeSets) {
        if (!changeSets || changeSets.length === 0) {
            return html`<span class="empty">No changesets.</span>`;
        }
        return html`
            <vaadin-grid .items="${changeSets}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-sort-column auto-width header="Id" path="id" frozen></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Author" path="author"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="File" path="filePath"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Description" path="description"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Status" path="status"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Last executed" path="dateLastExecuted"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Checksum" path="checksum"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }
}
customElements.define('pwc-liquibase-changesets', PwcLiquibaseChangesets);
