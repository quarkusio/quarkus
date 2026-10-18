import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-selection-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { notifier } from 'notifier';
import { msg, str, updateWhenLocaleChanges } from 'localization';

export class HibernateSearchOrmElasticsearchIndexedEntitiesComponent extends LitElement {

    static styles = css`
        .full-height {
          height: 100%;
        }
    `;

    jsonRpc = new JsonRpc(this);

    static properties = {
        _persistenceUnits: { state: true, type: Array },
        _selectedEntityTypes: { state: true, type: Object }
    }

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._persistenceUnits = null;
        this._selectedEntityTypes = {};
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
            return html`<span>${msg('Loading...', {
                id: 'quarkus-hibernate-search-orm-elasticsearch-loading'
            })}</span>`;
        }
    }

    _renderAllPUs() {
        return this._persistenceUnits.length === 0
            ? html`<p>${msg('No persistence units were found.', {
                id: 'quarkus-hibernate-search-orm-elasticsearch-no-persistence-units'
            })}</p>`
            : html`
                <vaadin-tabsheet class="full-height">
                    <span slot="prefix">
                        ${msg('Persistence Unit', {
                            id: 'quarkus-hibernate-search-orm-elasticsearch-persistence-unit'
                        })}
                    </span>
                    <vaadin-tabs slot="tabs">
                        ${this._persistenceUnits.map((pu) => html`
                            <vaadin-tab id="pu-${pu.name}-indexed-entity-types">
                                <span>${pu.name}</span>
                                <qui-badge small>
                                    <span>${pu.indexedEntities.length}</span>
                                </qui-badge>
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
        if (pu.indexedEntities.length === 0) {
            return html`<p>${msg('No indexed entities were found.', {
                id: 'quarkus-hibernate-search-orm-elasticsearch-no-indexed-entities'
            })}</p>`;
        }

        const selectedCount = this._selectedEntityTypes[pu.name].length;

        return html`
            <vaadin-horizontal-layout theme="spacing padding filled" style="align-items: baseline">
                <span>
                    ${msg(
                        str`Selected ${selectedCount} entity types`,
                        { id: 'quarkus-hibernate-search-orm-elasticsearch-selected-entity-types' }
                    )}
                </span>
                <vaadin-button
                    @click="${() => this._reindexSelected(pu.name)}"
                    ?disabled=${selectedCount === 0}>
                    <vaadin-icon icon="font-awesome-solid:rotate-right" slot="prefix"></vaadin-icon>
                    ${msg('Reindex selected', {
                        id: 'quarkus-hibernate-search-orm-elasticsearch-reindex-selected'
                    })}
                </vaadin-button>
            </vaadin-horizontal-layout>
            <vaadin-grid
                .items="${pu.indexedEntities}"
                .selectedItems="${this._selectedEntityTypes[pu.name].slice()}"
                @selected-items-changed="${(e) =>
                    this._selectEntityTypes(pu.name, e.target.selectedItems)}"
                class="datatable"
                theme="no-border row-stripes">
                <vaadin-grid-selection-column auto-select>
                </vaadin-grid-selection-column>
                <vaadin-grid-column
                    auto-width
                    header="${msg('Entity name', {
                        id: 'quarkus-hibernate-search-orm-elasticsearch-entity-name'
                    })}"
                    path="jpaName">
                </vaadin-grid-column>
                <vaadin-grid-column
                    auto-width
                    header="${msg('Class name', {
                        id: 'quarkus-hibernate-search-orm-elasticsearch-class-name'
                    })}"
                    path="javaClass">
                </vaadin-grid-column>
                <vaadin-grid-column
                    auto-width
                    header="${msg('Index names', {
                        id: 'quarkus-hibernate-search-orm-elasticsearch-index-names'
                    })}"
                    ${columnBodyRenderer(this._indexNameRenderer, [])}>
                </vaadin-grid-column>
            </vaadin-grid>`;
    }

    _indexNameRenderer(entity) {
        return entity.indexNames.map(
            (indexName) => html`<qui-badge>${indexName}</qui-badge>`
        );
    }

    _selectEntityTypes(puName, selectedItems) {
        if (this._selectedEntityTypes[puName].toString() !== selectedItems.toString()) {
            this._selectedEntityTypes[puName] = selectedItems.slice();
            this.requestUpdate();
        }
    }

    _reindexSelected(puName) {
        const selected = this._selectedEntityTypes[puName];
        if (!selected || selected.length === 0) {
            notifier.showErrorMessage(
                msg(
                    str`Select entity types to reindex for persistence unit '${this._escapeHTML(puName)}'.`,
                    { id: 'quarkus-hibernate-search-orm-elasticsearch-select-entity-types-to-reindex' }
                )
            );
            return;
        }
        const entityTypeNames = selected.map(e => e.jpaName);
        this.jsonRpc.reindex({ 'puName': puName, 'entityTypeNames': entityTypeNames })
            .onNext(response => {
                const status = response.result;
                const escapedPuName = this._escapeHTML(puName);
                const count = selected.length;

                if (status === 'started') {
                    notifier.showInfoMessage(
                        msg(
                            str`Requested reindexing of ${count} entity types for persistence unit '${escapedPuName}'.`,
                            { id: 'quarkus-hibernate-search-orm-elasticsearch-reindex-started' }
                        )
                    );
                } else if (status === 'success') {
                    notifier.showSuccessMessage(
                        msg(
                            str`Successfully reindexed ${count} entity types for persistence unit '${escapedPuName}'.`,
                            { id: 'quarkus-hibernate-search-orm-elasticsearch-reindex-success' }
                        )
                    );
                } else {
                    notifier.showErrorMessage(
                        msg(
                            str`An error occurred while reindexing ${count} entity types for persistence unit '${escapedPuName}':\n${status}`,
                            { id: 'quarkus-hibernate-search-orm-elasticsearch-reindex-error' }
                        )
                    );
                }
            });
    }

    _escapeHTML(text) {
        const fn = function (char) {
            const replacementMap = {
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;'
            };
            return replacementMap[char] || char;
        };
        return text.replace(/[&<>]/g, fn);
    }
}
customElements.define('hibernate-search-orm-elasticsearch-indexed-entity-types', HibernateSearchOrmElasticsearchIndexedEntitiesComponent);
