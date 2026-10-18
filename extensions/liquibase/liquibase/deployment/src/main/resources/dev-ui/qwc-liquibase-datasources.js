import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/confirm-dialog';
import '@vaadin/text-field';
import '@vaadin/text-area';
import '@vaadin/form-layout';
import '@vaadin/progress-bar';
import '@vaadin/checkbox';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import 'qui-alert';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { msg, str, updateWhenLocaleChanges } from 'localization';

export class QwcLiquibaseDatasources extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        .button {
            cursor: pointer;
        }
        .clearIcon {
            color: red;
        }
        .migrateIcon {
            color: yellow;
        }
        .message {
          padding: 15px;
          text-align: center;
          margin-left: 20%;
          margin-right: 20%;
          border: 2px solid orange;
          border-radius: 10px;
          font-size: large;
        }
        `;

    static properties = {
        "_factories": {state: true},
        "_ds": {state: true},
        "_message": {state: true},
        "_dialogOpened": {state: true}
    }

    constructor() {
        super();
        updateWhenLocaleChanges(this);
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getLiquibaseFactories().then(jsonRpcResponse => {
            this._factories = jsonRpcResponse.result;
        });
    }

    render() {
        if (this._factories) {
            return this._renderDataSourceTable();
        } else {
            return html`<span>${msg('Loading datasources...', { id: 'quarkus-liquibase-loading-datasources' })}</span>`;
        }
    }

    _renderDataSourceTable() {
        return html`
                ${this._message}
                <vaadin-grid .items="${this._factories}" class="datatable" theme="no-border">
                    <vaadin-grid-column auto-width
                                        header=${msg('Name', { id: 'quarkus-liquibase-name' })}
                                        ${columnBodyRenderer(this._nameRenderer, [])}>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header=${msg('Action', { id: 'quarkus-liquibase-action' })}
                                        ${columnBodyRenderer(this._actionRenderer, [])}
                                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>
                <vaadin-confirm-dialog
                  header=${msg('Clear Database', { id: 'quarkus-liquibase-clear-database' })}
                  cancel
                  confirm-text=${msg('Clear', { id: 'quarkus-liquibase-clear' })}
                  .opened="${this._dialogOpened}"
                  @confirm="${() => {
            this._clear(this._ds);
        }}"
                  @cancel="${() => {
            this._dialogOpened = false;
        }}"
                >
                  ${msg('This will drop all objects (tables, views, procedures, triggers, ...) in the configured schema. Do you want to continue?', { id: 'quarkus-liquibase-clear-confirm' })}
                </vaadin-confirm-dialog>
            `;
    }

    _actionRenderer(ds) {
        return html`
            <vaadin-button theme="primary small" @click=${() => this._confirm(ds)} class="button">
                <vaadin-icon class="clearIcon" icon="font-awesome-solid:power-off"></vaadin-icon> ${msg('Clear', { id: 'quarkus-liquibase-clear' })}
            </vaadin-button>
            <vaadin-button theme="primary small" @click=${() => this._migrate(ds)} class="button">
                <vaadin-icon class="migrateIcon" icon="font-awesome-solid:bolt-lightning"></vaadin-icon> ${msg('Migrate', { id: 'quarkus-liquibase-migrate' })}
            </vaadin-button>
       `;
    }

    _nameRenderer(ds) {
        return html`${ds.dataSourceName}`;
    }

    _confirm(ds) {
        this._message = '';
        this._ds = ds;
        this._dialogOpened = true;
    }

    _clear(ds) {
        const dsn = ds.dataSourceName;
        this._message = '';
        this.jsonRpc.clear({ds: ds.dataSourceName}).then(jsonRpcResponse => {
            this._message = html`<qui-alert level="success" showIcon>
                                    <span>${msg(str`The datasource ${dsn} has been cleared.`, { id: 'quarkus-liquibase-cleared' })}</span>
                                 </qui-alert>`;
        });
        this._ds = null;
    }

    _migrate(ds) {
        const dsn = ds.dataSourceName;
        this._message = '';
        this.jsonRpc.migrate({ds: ds.dataSourceName}).then(jsonRpcResponse => {
            this._message = html`<qui-alert level="success" showIcon>
                                    <span>${msg(str`The datasource ${dsn} has been migrated.`, { id: 'quarkus-liquibase-migrated' })}</span>
                                 </qui-alert>`;
        });
    }

}
customElements.define('qwc-liquibase-datasources', QwcLiquibaseDatasources);