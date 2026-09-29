import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the Flyway migration history. For each datasource it
 * shows the applied and pending migrations. No migrate / clean / repair actions
 * and no credentials are exposed.
 */
export class PwcFlywayMigrations extends LitElement {

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
        this.jsonRpc.getMigrations().then(jsonRpcResponse => {
            this._datasources = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._datasources) {
            return html`<span class="empty">Loading Flyway migrations...</span>`;
        }
        if (this._datasources.length === 0) {
            return html`<span class="empty">No Flyway datasources configured.</span>`;
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
                    : this._renderMigrations(ds.migrations)}
            </div>`;
    }

    _renderMigrations(migrations) {
        if (!migrations || migrations.length === 0) {
            return html`<span class="empty">No migrations.</span>`;
        }
        return html`
            <vaadin-grid .items="${migrations}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-sort-column auto-width header="Version" path="version" frozen></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Description" path="description"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Type" path="type"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="State" path="state"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Installed on" path="installedOn"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Checksum" path="checksum"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Exec (ms)" path="executionTime"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }
}
customElements.define('pwc-flyway-migrations', PwcFlywayMigrations);
