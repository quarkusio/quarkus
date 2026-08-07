import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/progress-bar';
import '@vaadin/tooltip';
import 'qui-alert';
import { notifier } from 'notifier';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { msg, updateWhenLocaleChanges } from 'localization';

export class QwcFlywayMongodbClients extends QwcHotReloadElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        .actions {
            display: flex;
            gap: 0.5em;
        }
        vaadin-grid {
            height: 100%;
        }`;

    static properties = {
        _clients: { state: true },
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._clients = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload() {
        this.jsonRpc.getClients().then(r => {
            this._clients = r.result;
        });
    }

    render() {
        if (!this._clients) {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
        return html`
            <vaadin-grid .items="${this._clients}" theme="row-stripes">
                <vaadin-grid-sort-column path="name" header="${msg('Client', { id: 'quarkus-flyway-mongodb-client' })}" auto-width></vaadin-grid-sort-column>
                <vaadin-grid-sort-column path="appliedCount" header="${msg('Applied', { id: 'quarkus-flyway-mongodb-applied' })}" auto-width></vaadin-grid-sort-column>
                <vaadin-grid-sort-column path="pendingCount" header="${msg('Pending', { id: 'quarkus-flyway-mongodb-pending' })}" auto-width></vaadin-grid-sort-column>
                <vaadin-grid-sort-column path="currentVersion" header="${msg('Current version', { id: 'quarkus-flyway-mongodb-current-version' })}" auto-width></vaadin-grid-sort-column>
                <vaadin-grid-column header="${msg('Actions', { id: 'quarkus-flyway-mongodb-actions' })}" ${columnBodyRenderer(this._renderActions, [])}></vaadin-grid-column>
            </vaadin-grid>`;
    }

    _renderActions = (client) => {
        if (!client.hasMigrations) {
            return html``;
        }
        const cleanColorVar = client.cleanDisabled ? '--lumo-disabled-text-color' : '--lumo-error-text-color';
        return html`
            <span class="actions">
                <vaadin-button theme="small" @click="${() => this._migrate(client.name)}">
                    <vaadin-icon icon="font-awesome-solid:play" slot="prefix"></vaadin-icon>
                    ${msg('Migrate', { id: 'quarkus-flyway-mongodb-migrate' })}
                </vaadin-button>
                <div id="clean-${client.name}" style="display: inline-block;">
                    <vaadin-button theme="small" ?disabled="${client.cleanDisabled}" @click="${() => this._clean(client.name)}">
                        <vaadin-icon style="color: var(${cleanColorVar});" icon="font-awesome-solid:broom" slot="prefix"></vaadin-icon>
                        ${msg('Clean', { id: 'quarkus-flyway-mongodb-clean' })}
                    </vaadin-button>
                </div>
                ${client.cleanDisabled ? html`
                    <vaadin-tooltip for="clean-${client.name}"
                        text="${msg('Flyway MongoDB clean has been disabled via quarkus.flyway-mongodb.clean-disabled=true', { id: 'quarkus-flyway-mongodb-clean-disabled-tooltip' })}">
                    </vaadin-tooltip>` : null}
            </span>`;
    };

    _migrate(client) {
        this.jsonRpc.migrate({ client }).then(r => {
            this._showResultNotification(r.result);
            this.hotReload();
        });
    }

    _clean(client) {
        if (confirm(msg('This will drop all objects (collections, indexes, ...) in the configured database. Do you want to continue?', { id: 'quarkus-flyway-mongodb-clean-confirm' }))) {
            this.jsonRpc.clean({ client }).then(r => {
                this._showResultNotification(r.result);
                this.hotReload();
            });
        }
    }

    _showResultNotification(response) {
        if (response.type === 'success') {
            notifier.showInfoMessage(response.message);
        } else {
            notifier.showWarningMessage(response.message);
        }
    }
}

customElements.define('qwc-flyway-mongodb-clients', QwcFlywayMongodbClients);
