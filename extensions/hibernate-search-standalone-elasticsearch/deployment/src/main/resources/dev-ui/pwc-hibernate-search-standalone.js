import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the standalone Hibernate Search indexed entity
 * types. It lists the indexed entities and their index names. No reindex /
 * mass-indexer action and no Elasticsearch host or credentials are exposed.
 */
export class PwcHibernateSearchStandalone extends LitElement {

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
        .index-name {
            display: inline-block;
            margin-right: 6px;
            padding: 1px 8px;
            border-radius: var(--lumo-border-radius-s);
            background-color: var(--lumo-contrast-10pct);
            font-size: var(--lumo-font-size-s);
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _indexedEntities: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getIndexedEntities().then(jsonRpcResponse => {
            this._indexedEntities = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._indexedEntities) {
            return html`<span class="empty">Loading Hibernate Search indexed entities...</span>`;
        }
        if (this._indexedEntities.length === 0) {
            return html`<span class="empty">No indexed entities.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-grid .items="${this._indexedEntities}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-sort-column auto-width header="Entity name" path="name" frozen></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Class name" path="javaClass"></vaadin-grid-sort-column>
                <vaadin-grid-column auto-width header="Index names"
                    ${columnBodyRenderer(this._indexNamesRenderer, [])}></vaadin-grid-column>
            </vaadin-grid>`;
    }

    _indexNamesRenderer(entity) {
        return html`${entity.indexNames.map(indexName => html`<span class="index-name">${indexName}</span>`)}`;
    }
}
customElements.define('pwc-hibernate-search-standalone', PwcHibernateSearchStandalone);
