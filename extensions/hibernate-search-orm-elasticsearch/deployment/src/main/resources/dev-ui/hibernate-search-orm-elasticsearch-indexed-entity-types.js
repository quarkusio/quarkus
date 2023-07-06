import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-selection-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { notifier } from 'notifier';

export class HibernateSearchOrmElasticsearchIndexedEntitiesComponent extends LitElement {

    static styles = css`
        .full-height {
          height: 100%;
        }
    `;

    jsonRpc = new JsonRpc(this);

    static properties = {
        _persistenceUnits: {state: true, type: Array},
        _selectedEntityTypes: {state: true, type: Object}
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getInfo().then(response => {
            this._persistenceUnits = response.result.persistenceUnits;
            this._selectedEntityTypes = {};
            this._persistenceUnits.forEach(pu => {
                this._selectedEntityTypes[pu.name] = [];
            });
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
                                <vaadin-tab id="pu-${pu.name}-indexed-entity-types">
                                    <span>${pu.name}</span>
                                    <qui-badge small><span>${pu.indexedEntities.length}</span></qui-badge>
                                </vaadin-tab>
                                `)}
                        </vaadin-tabs>

                        ${this._persistenceUnits.map((pu) => html`
                            <div class="full-height" tab="pu-${pu.name}-indexed-entity-types">
                                ${this._renderEntityTypesTable(pu)}
                            </div>
                            `)}
                    </vaadin-tabsheet>`;
    }

    _renderEntityTypesTable(pu) {
        if (pu.indexedEntities.length == 0) {
            return html`<p>No indexed entities were found.</p>`
        }
        return html`
                <vaadin-horizontal-layout theme="spacing padding filled" style="align-items: baseline">
                    <span>Selected ${this._selectedEntityTypes[pu.name].length} entity type${this._selectedEntityTypes[pu.name].length != 1 ? 's' : ''}</span>
                    <vaadin-button @click="${() => this._reindexSelected(pu.name)}"
                        ?disabled=${this._selectedEntityTypes[pu.name].length == 0}>
                        <vaadin-icon icon="font-awesome-solid:rotate-right" slot="prefix"></vaadin-icon>
                        Reindex selected
                    </vaadin-button>
                </vaadin-horizontal-layout>
                <vaadin-grid .items="${pu.indexedEntities}"
                        .selectedItems="${this._selectedEntityTypes[pu.name].slice()}"
                        @selected-items-changed="${(e) => this._selectEntityTypes(pu.name, e.target.selectedItems)}"
                        class="datatable" theme="no-border row-stripes">
                    <vaadin-grid-selection-column auto-select>
                    </vaadin-grid-selection-column>
                    <vaadin-grid-column auto-width
                                        header="Entity name"
                                        path="jpaName">
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

    _selectEntityTypes(puName, selectedItems) {
        if (this._selectedEntityTypes[puName].toString() !== selectedItems.toString()) {
            this._selectedEntityTypes[puName] = selectedItems.slice();
            this.requestUpdate();
        }
    }

    _reindexSelected(puName) {
        const selected = this._selectedEntityTypes[puName];
        if (selected == null || selected.length == 0) {
            notifier.showErrorMessage("Select entity types to reindex for persistence unit '" + puName + "'.");
            return;
        }
        const entityTypeNames = selected.map(e => e.jpaName);
        this.jsonRpc.reindex({'puName': puName, 'entityTypeNames': entityTypeNames})
            .onNext(response => {
                const msg = response.result;
                if (msg === "started") {
                    notifier.showInfoMessage("Requested reindexing of " + selected.length
                            + " entity types for persistence unit '" + this._escapeHTML(puName) + "'.");
                } else if (msg === "success") {
                    notifier.showSuccessMessage("Successfully reindexed " + selected.length
                            + " entity types for persistence unit '" + this._escapeHTML(puName) + "'.");
                } else {
                    notifier.showErrorMessage("An error occurred while reindexing " + selected.length
                            + " entity types for persistence unit '" + this._escapeHTML(puName) + "':\n" + msg);
                }
            });
    }

    _escapeHTML(text) {
        var fn=function(char) {
            var replacementMap = {
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;'
            };
            return replacementMap[char] || char;
        }
        return text.replace(/[&<>]/g, fn);
    }
}
customElements.define('hibernate-search-orm-elasticsearch-indexed-entity-types', HibernateSearchOrmElasticsearchIndexedEntitiesComponent);