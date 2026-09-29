import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the Hibernate Search indexed entity types. For each
 * persistence unit it lists the indexed entities and their index names. No
 * reindex / mass-indexer action and no Elasticsearch host or credentials are
 * exposed.
 */
export class PwcHibernateSearch extends LitElement {

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
        .persistence-unit {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        .persistence-unit-name {
            font-size: var(--lumo-font-size-l);
            font-weight: bold;
        }
        .index-name {
            display: inline-block;
            margin-right: 6px;
            padding: 1px 8px;
            border-radius: var(--lumo-border-radius-s);
            background-color: var(--lumo-contrast-10pct);
            font-size: var(--lumo-font-size-s);
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
        _persistenceUnits: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getPersistenceUnits().then(jsonRpcResponse => {
            this._persistenceUnits = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._persistenceUnits) {
            return html`<span class="empty">Loading Hibernate Search indexed entities...</span>`;
        }
        if (this._persistenceUnits.length === 0) {
            return html`<span class="empty">No Hibernate Search persistence units configured.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            ${this._persistenceUnits.map(pu => this._renderPersistenceUnit(pu))}`;
    }

    _renderPersistenceUnit(pu) {
        return html`
            <div class="persistence-unit">
                <span class="persistence-unit-name">${pu.name}</span>
                ${pu.error
                    ? html`<span class="error">${pu.error}</span>`
                    : this._renderIndexedEntities(pu.indexedEntities)}
            </div>`;
    }

    _renderIndexedEntities(indexedEntities) {
        if (!indexedEntities || indexedEntities.length === 0) {
            return html`<span class="empty">No indexed entities.</span>`;
        }
        return html`
            <vaadin-grid .items="${indexedEntities}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-sort-column auto-width header="Entity name" path="jpaName" frozen></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Class name" path="javaClass"></vaadin-grid-sort-column>
                <vaadin-grid-column auto-width header="Index names"
                    ${columnBodyRenderer(this._indexNamesRenderer, [])}></vaadin-grid-column>
            </vaadin-grid>`;
    }

    _indexNamesRenderer(entity) {
        return html`${entity.indexNames.map(indexName => html`<span class="index-name">${indexName}</span>`)}`;
    }
}
customElements.define('pwc-hibernate-search', PwcHibernateSearch);
