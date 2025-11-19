import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-selection-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { notifier } from 'notifier';
import { msg, str, updateWhenLocaleChanges } from 'localization';

export class HibernateSearchStandaloneElasticsearchIndexedEntitiesComponent extends LitElement {

    static styles = css`
        .full-height {
          height: 100%;
        }
    `;

    jsonRpc = new JsonRpc(this);

    static properties = {
        _indexedEntityTypes: {state: true, type: Array},
        _selectedEntityTypes: {state: true, type: Array}
    }

    constructor() {
        super();
        updateWhenLocaleChanges(this);
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getInfo().then(response => {
            this._indexedEntityTypes = response.result.indexedEntities;
            this._selectedEntityTypes = [];
        });
    }

    render() {
        if (this._indexedEntityTypes) {
            return this._renderEntityTypesTable();
        } else {
            return html`<span>${msg('Loading...', { id: 'quarkus-hibernate-search-standalone-elasticsearch-loading' })}</span>`;
        }
    }

    _renderEntityTypesTable() {
        if (this._indexedEntityTypes.length === 0) {
            return html`<p>${msg('No indexed entities were found.', { id: 'quarkus-hibernate-search-standalone-elasticsearch-no-indexed-entities' })}</p>`;
        }
        const l = this._selectedEntityTypes.length;
        const t = this._selectedEntityTypes.length !== 1 ? 's' : '';
        return html`
                <vaadin-horizontal-layout theme="spacing padding filled" style="align-items: baseline">
                    <span>${msg(str`Selected ${l} entity type${t}`, { id: 'quarkus-hibernate-search-standalone-elasticsearch-selected-entity-types' })}</span>
                    <vaadin-button @click="${() => this._reindexSelected()}"
                        ?disabled=${this._selectedEntityTypes.length === 0}>
                        <vaadin-icon icon="font-awesome-solid:rotate-right" slot="prefix"></vaadin-icon>
                        ${msg('Reindex selected', { id: 'quarkus-hibernate-search-standalone-elasticsearch-reindex-selected' })}
                    </vaadin-button>
                </vaadin-horizontal-layout>
                <vaadin-grid .items="${this._indexedEntityTypes}"
                        .selectedItems="${this._selectedEntityTypes.slice()}"
                        @selected-items-changed="${(e) => this._selectEntityTypes(e.target.selectedItems)}"
                        class="datatable" theme="no-border row-stripes">
                    <vaadin-grid-selection-column auto-select>
                    </vaadin-grid-selection-column>
                    <vaadin-grid-column auto-width
                                        header=${msg('Entity name', { id: 'quarkus-hibernate-search-standalone-elasticsearch-entity-name' })}
                                        path="name">
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header=${msg('Class name', { id: 'quarkus-hibernate-search-standalone-elasticsearch-class-name' })}
                                        path="javaClass">
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header=${msg('Index names', { id: 'quarkus-hibernate-search-standalone-elasticsearch-index-names' })}
                                        ${columnBodyRenderer(this._indexNameRenderer, [])}>
                    </vaadin-grid-column>
                </vaadin-grid>`;
    }

    _indexNameRenderer(entity) {
        return entity.indexNames.map((indexName) => html`<qui-badge>${indexName}</qui-badge>`);
    }

    _selectEntityTypes(selectedItems) {
        if (this._selectedEntityTypes.toString() !== selectedItems.toString()) {
            this._selectedEntityTypes = selectedItems.slice();
            this.requestUpdate();
        }
    }

    _reindexSelected() {
        const selected = this._selectedEntityTypes;
        if (selected === null || selected.length === 0) {
            notifier.showErrorMessage(msg('Select entity types to reindex.', { id: 'quarkus-hibernate-search-standalone-elasticsearch-select-entity-types' }));
            return;
        }
        const entityTypeNames = selected.map(e => e.name);
        const l = selected.length;
        this.jsonRpc.reindex({'entityTypeNames': entityTypeNames})
            .onNext(response => {
                const resultMsg = response.result;
                if (resultMsg === "started") {
                    notifier.showInfoMessage(msg(str`Requested reindexing of ${l} entity types.`, { id: 'quarkus-hibernate-search-standalone-elasticsearch-reindex-started' }));
                } else if (resultMsg === "success") {
                    notifier.showSuccessMessage(msg(str`Successfully reindexed ${l} entity types.`, { id: 'quarkus-hibernate-search-standalone-elasticsearch-reindex-success' }));
                } else {
                    notifier.showErrorMessage(msg(str`An error occurred while reindexing ${l} entity types:\n${resultMsg}`, { id: 'quarkus-hibernate-search-standalone-elasticsearch-reindex-error' }));
                }
            });
    }
}
customElements.define('hibernate-search-standalone-elasticsearch-indexed-entity-types', HibernateSearchStandaloneElasticsearchIndexedEntitiesComponent);