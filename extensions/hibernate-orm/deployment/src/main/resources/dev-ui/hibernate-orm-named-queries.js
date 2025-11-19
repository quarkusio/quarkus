import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/progress-bar';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { notifier } from 'notifier';
import { msg, str, updateWhenLocaleChanges } from 'localization';

export class HibernateOrmNamedQueriesComponent extends QwcHotReloadElement {

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
        _persistenceUnits: { state: true, type: Array }
    }

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._persistenceUnits = [];
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload() {
        this.jsonRpc.getInfo().then(response => {
            this._persistenceUnits = response.result.persistenceUnits;
        }).catch(error => {
            console.error('Failed to fetch persistence units:', error);
            this._persistenceUnits = [];
            notifier.showErrorMessage(
                msg(
                    str`Failed to fetch persistence units: ${0}`,
                    {
                        id: 'quarkus-hibernate-orm-failed-to-fetch',
                        args: [String(error)]
                    }
                ),
                'bottom-start',
                30
            );
        });
    }

    render() {
        if (this._persistenceUnits) {
            return this._renderAllPUs();
        } else {
            return html`
                <div style="color: var(--lumo-secondary-text-color);width: 95%;">
                    <div>
                        ${msg('Fetching persistence units...', {
                            id: 'quarkus-hibernate-orm-fetching-persistence-units'
                        })}
                    </div>
                    <vaadin-progress-bar indeterminate></vaadin-progress-bar>
                </div>`;
        }
    }

    _renderAllPUs() {
        return this._persistenceUnits.length === 0
            ? html`
                <p>
                    ${msg('No persistence units were found.', {
                        id: 'quarkus-hibernate-orm-no-persistence-units'
                    })}
                    <vaadin-button @click="${this.hotReload}" theme="small">
                        ${msg('Check again', {
                            id: 'quarkus-hibernate-orm-check-again'
                        })}
                    </vaadin-button>
                </p>`
            : html`
                <vaadin-tabsheet class="full-height">
                    <span slot="prefix">
                        ${msg('Persistence Unit', {
                            id: 'quarkus-hibernate-orm-persistence-unit'
                        })}
                    </span>
                    <vaadin-tabs slot="tabs">
                        ${this._persistenceUnits.map((pu) => html`
                            <vaadin-tab id="pu-${pu.name}-named-queries">
                                <span>${pu.name}</span>
                                <qui-badge small>
                                    <span>${pu.namedQueries.length}</span>
                                </qui-badge>
                            </vaadin-tab>
                        `)}
                    </vaadin-tabs>

                    ${this._persistenceUnits.map((pu) => html`
                        <div class="full-height" tab="pu-${pu.name}-named-queries">
                            ${this._renderNamedQueriesTable(pu)}
                        </div>
                    `)}
                </vaadin-tabsheet>`;
    }

    _renderNamedQueriesTable(pu) {
        if (pu.namedQueries.length === 0) {
            return html`
                <p>
                    ${msg('No named queries were found.', {
                        id: 'quarkus-hibernate-orm-no-named-queries'
                    })}
                    <vaadin-button @click="${this.hotReload}" theme="small">
                        ${msg('Check again', {
                            id: 'quarkus-hibernate-orm-check-again'
                        })}
                    </vaadin-button>
                </p>`;
        }
        return html`
            <vaadin-grid
                .items="${pu.namedQueries}"
                class="datatable"
                theme="no-border row-stripes">

                <vaadin-grid-column
                    auto-width
                    header="${msg('Name', { id: 'quarkus-hibernate-orm-name' })}"
                    path="name">
                </vaadin-grid-column>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Query', { id: 'quarkus-hibernate-orm-query' })}"
                    path="query">
                </vaadin-grid-column>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Lock Mode', { id: 'quarkus-hibernate-orm-lock-mode' })}"
                    path="lockMode">
                </vaadin-grid-column>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Cacheable', { id: 'quarkus-hibernate-orm-cacheable' })}"
                    path="cacheable">
                </vaadin-grid-column>

                <vaadin-grid-column
                    auto-width
                    header="${msg('Query Type', { id: 'quarkus-hibernate-orm-query-type' })}"
                    path="type">
                </vaadin-grid-column>
            </vaadin-grid>`;
    }

}
customElements.define('hibernate-orm-named-queries', HibernateOrmNamedQueriesComponent);
