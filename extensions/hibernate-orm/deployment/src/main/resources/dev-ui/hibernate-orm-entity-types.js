import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/combo-box';
import '@vaadin/grid';
import '@vaadin/progress-bar';
import { notifier } from 'notifier';
import { msg, str, updateWhenLocaleChanges } from 'localization';

export class HibernateOrmEntityTypesComponent extends QwcHotReloadElement {

    static styles = css`
        :host {
            display: flex;
            padding-left: 10px;
            padding-right: 10px;
        }
        .full-height {
            height: 100%;
            width: 100%;
        }
    `;

    jsonRpc = new JsonRpc(this);

    static properties = {
        _persistenceUnits: { state: true, type: Array },
        _selectedPersistenceUnit: { state: true }
    }

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._persistenceUnits = null;
        this._selectedPersistenceUnit = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload() {
        this.jsonRpc.getInfo().then(response => {
            this._persistenceUnits = response.result.persistenceUnits;
            this._selectedPersistenceUnit = this._persistenceUnits[0] ?? null;
        }).catch(error => {
            console.error("Failed to fetch persistence units:", error);
            this._persistenceUnits = [];
            notifier.showErrorMessage(
                msg(
                    str`Failed to fetch persistence units: ${String(error)}`,
                    {
                        id: 'quarkus-hibernate-orm-failed-to-fetch',
                    }
                ),
                "bottom-start",
                30
            );
        });
    }

    render() {
        if (this._persistenceUnits === null) {
            return html`
                <div style="color: var(--lumo-secondary-text-color);width: 95%;">
                    <div>${msg('Fetching persistence units…', { id: 'quarkus-hibernate-orm-fetching-persistence-units' })}</div>
                    <vaadin-progress-bar indeterminate></vaadin-progress-bar>
                </div>`;
        }
        return this._renderAllPUs();
    }

    _renderAllPUs() {
        if (this._persistenceUnits.length === 0) {
            return html`
                <p>
                    ${msg('No persistence units found', { id: 'quarkus-hibernate-orm-no-persistence-units' })}
                    <vaadin-button @click="${this.hotReload}" theme="small">
                        ${msg('Check again', { id: 'quarkus-hibernate-orm-check-again' })}
                    </vaadin-button>
                </p>`;
        }
        return html`
            <div class="full-height">
                ${this._persistenceUnits.length > 1 ? html`
                    <vaadin-combo-box
                        label="${msg('Persistence Unit', { id: 'quarkus-hibernate-orm-persistence-unit' })}"
                        item-label-path="name"
                        item-value-path="name"
                        .items="${this._persistenceUnits}"
                        .value="${this._selectedPersistenceUnit?.name || ''}"
                        @value-changed="${this._onPersistenceUnitChanged}"
                        .allowCustomValue="${false}">
                    </vaadin-combo-box>` : ''}
                ${this._selectedPersistenceUnit
                    ? this._renderEntityTypesTable(this._selectedPersistenceUnit)
                    : html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`}
            </div>`;
    }

    _onPersistenceUnitChanged(event) {
        const selected = this._persistenceUnits.find(pu => pu.name === event.detail.value);
        if (selected) {
            this._selectedPersistenceUnit = selected;
        }
    }

    _renderEntityTypesTable(pu) {
        if (pu.managedEntities.length === 0) {
            return html`
                <p>${msg('No managed entities', { id: 'quarkus-hibernate-orm-no-managed-entities' })}</p>
            `;
        }
        return html`
            <vaadin-grid .items="${pu.managedEntities}" class="datatable" theme="no-border row-stripes">
                <vaadin-grid-column auto-width
                    header="${msg('JPA Entity Name', { id: 'quarkus-hibernate-orm-jpa-entity-name' })}"
                    path="name">
                </vaadin-grid-column>
                <vaadin-grid-column auto-width
                    header="${msg('Class Name', { id: 'quarkus-hibernate-orm-class-name' })}"
                    path="className">
                </vaadin-grid-column>
                <vaadin-grid-column auto-width
                    header="${msg('Table Name', { id: 'quarkus-hibernate-orm-table-name' })}"
                    path="tableName">
                </vaadin-grid-column>
            </vaadin-grid>`;
    }

}
customElements.define('hibernate-orm-entity-types', HibernateOrmEntityTypesComponent);
