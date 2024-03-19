import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-selection-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { notifier } from 'notifier';

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
            return html`<span>Loading...</span>`;
        }
    }

    _renderEntityTypesTable() {
        if (this._indexedEntityTypes.length == 0) {
            return html`<p>No indexed entities were found.</p>`
        }
        return html`
                <vaadin-horizontal-layout theme="spacing padding filled" style="align-items: baseline">
                    <span>Selected ${this._selectedEntityTypes.length} entity type${this._selectedEntityTypes.length != 1 ? 's' : ''}</span>
                    <vaadin-button @click="${() => this._reindexSelected()}"
                        ?disabled=${this._selectedEntityTypes.length == 0}>
                        <vaadin-icon icon="font-awesome-solid:rotate-right" slot="prefix"></vaadin-icon>
                        Reindex selected
                    </vaadin-button>
                </vaadin-horizontal-layout>
                <vaadin-grid .items="${this._indexedEntityTypes}"
                        .selectedItems="${this._selectedEntityTypes.slice()}"
                        @selected-items-changed="${(e) => this._selectEntityTypes(e.target.selectedItems)}"
                        class="datatable" theme="no-border row-stripes">
                    <vaadin-grid-selection-column auto-select>
                    </vaadin-grid-selection-column>
                    <vaadin-grid-column auto-width
                                        header="Entity name"
                                        path="name">
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header="Class name"
                                        path="javaClass">
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header="Index names"
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
        if (selected == null || selected.length == 0) {
            notifier.showErrorMessage("Select entity types to reindex.");
            return;
        }
        const entityTypeNames = selected.map(e => e.name);
        this.jsonRpc.reindex({'entityTypeNames': entityTypeNames})
            .onNext(response => {
                const msg = response.result;
                if (msg === "started") {
                    notifier.showInfoMessage("Requested reindexing of " + selected.length
                            + " entity types.");
                } else if (msg === "success") {
                    notifier.showSuccessMessage("Successfully reindexed " + selected.length
                            + " entity types.");
                } else {
                    notifier.showErrorMessage("An error occurred while reindexing " + selected.length
                            + " entity types:\n" + msg);
                }
            });
    }
}
customElements.define('hibernate-search-standalone-elasticsearch-indexed-entity-types', HibernateSearchStandaloneElasticsearchIndexedEntitiesComponent);