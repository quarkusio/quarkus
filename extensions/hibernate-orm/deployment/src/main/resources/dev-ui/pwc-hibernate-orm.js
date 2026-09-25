import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the Hibernate ORM persistence units. For each unit
 * it lists the managed entities and the registered named queries. No HQL console,
 * no DDL scripts and no credentials are exposed.
 */
export class PwcHibernateOrm extends LitElement {

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
        .section-header {
            font-size: var(--lumo-font-size-m);
            font-weight: bold;
            color: var(--lumo-secondary-text-color);
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
            return html`<span class="empty">Loading Hibernate ORM persistence units...</span>`;
        }
        if (this._persistenceUnits.length === 0) {
            return html`<span class="empty">No Hibernate ORM persistence units configured.</span>`;
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
                    : html`
                        <span class="section-header">Entities</span>
                        ${this._renderEntities(pu.entities)}
                        <span class="section-header">Named Queries</span>
                        ${this._renderNamedQueries(pu.namedQueries)}`}
            </div>`;
    }

    _renderEntities(entities) {
        if (!entities || entities.length === 0) {
            return html`<span class="empty">No managed entities.</span>`;
        }
        return html`
            <vaadin-grid .items="${entities}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-sort-column auto-width header="Name" path="name" frozen></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Class" path="className"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Table" path="tableName"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }

    _renderNamedQueries(namedQueries) {
        if (!namedQueries || namedQueries.length === 0) {
            return html`<span class="empty">No named queries.</span>`;
        }
        return html`
            <vaadin-grid .items="${namedQueries}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-sort-column auto-width header="Name" path="name" frozen></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Query" path="query"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Type" path="type"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Cacheable" path="cacheable"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Lock mode" path="lockMode"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }
}
customElements.define('pwc-hibernate-orm', PwcHibernateOrm);
