import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/text-area';
import '@vaadin/form-layout';
import '@vaadin/progress-bar';
import '@vaadin/tooltip';
import '@vaadin/checkbox';
import '@vaadin/grid';
import 'qui-alert';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { dialogRenderer } from '@vaadin/dialog/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/progress-bar';
import { notifier } from 'notifier';
import { msg, str, updateWhenLocaleChanges } from 'localization';

export class QwcFlywayDatasources extends QwcHotReloadElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        .button {
            cursor: pointer;
        }`;

    static properties = {
        _ds: {state: true},
        _selectedDs: {state: true},
        _createDialogOpened: {state: true},
        _cleanDisabled: {state: true}
    }
    
    constructor() { 
        super();
        updateWhenLocaleChanges(this);
        this._ds = null;
        this._selectedDs = null;
        this._createDialogOpened = false;
        this._updateDialogOpened = false;
        this._cleanDisabled = true;
    }    
    
    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload(){
        this.jsonRpc.getDatasources().then(jsonRpcResponse => {
            this._ds = jsonRpcResponse.result;
        });
        
        this.jsonRpc.isCleanDisabled().then(jsonRpcResponse => {
            this._cleanDisabled = jsonRpcResponse.result;
        });
    }

    render() {
        if (this._ds) {
            return this._renderDataSourceTable();
        } else {
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }
    }

    _renderDataSourceTable() {
        return html`${this._renderCreateDialog()}${this._renderUpdateDialog()}
                <vaadin-grid .items="${this._ds}" class="datatable" theme="no-border">
                    <vaadin-grid-column auto-width
                                        header=${msg('Name', { id: 'quarkus-flyway-name' })}
                                        ${columnBodyRenderer(this._nameRenderer, [])}>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header=${msg('Action', { id: 'quarkus-flyway-action' })}
                                        ${columnBodyRenderer(this._actionRenderer, [])}
                                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>`;
    }

    _actionRenderer(ds) {
        return html`${this._renderMigrationButtons(ds)}
                    ${this._renderUpdateButton(ds)}
                    ${this._renderCreateButton(ds)}`;
    }

    _renderMigrationButtons(ds) {
        if(ds.hasMigrations){
            let colorvar = this._cleanDisabled ? '--lumo-disabled-text-color' : '--lumo-warning-text-color';
            return html`<div id=${ds.name} style="display: inline-block;">
                <vaadin-button theme="small" @click=${() => this._clean(ds)} class="button" ?disabled=${this._cleanDisabled}>
                    <vaadin-icon style="color: var(${colorvar});" icon="font-awesome-solid:broom"></vaadin-icon> ${msg('Clean', { id: 'quarkus-flyway-clean' })}
                </vaadin-button></div>
                <vaadin-button theme="small" @click=${() => this._migrate(ds)} class="button">
                    <vaadin-icon icon="font-awesome-solid:arrow-right-arrow-left"></vaadin-icon> ${msg('Migrate', { id: 'quarkus-flyway-migrate' })}
                </vaadin-button>
                ${this._cleanDisabled
                    ? html`<vaadin-tooltip for="${ds.name}" text=${msg('Flyway clean has been disabled via quarkus.flyway.clean-disabled=true', { id: 'quarkus-flyway-clean-disabled-tooltip' })}></vaadin-tooltip>`
                    : null}
                `;
        }
    }

    _renderUpdateButton(ds) {
        if(ds.hasMigrations){
            return html`
                <vaadin-button theme="small" @click=${() => this._showUpdateDialog(ds)} class="button" title=${msg('Create update migration file. Always manually review the created file as it can cause data loss', { id: 'quarkus-flyway-update-button-title' })}>
                    <vaadin-icon icon="font-awesome-solid:plus"></vaadin-icon> ${msg('Generate Migration File', { id: 'quarkus-flyway-generate-migration-file' })}
                </vaadin-button>`;
        }
    }

    _renderCreateButton(ds) {
        if(ds.createPossible){
            return html`
                <vaadin-button theme="small" @click=${() => this._showCreateDialog(ds)} class="button" title=${msg('Set up basic files for Flyway migrations to work. Initial file in db/migrations will be created and you can then add additional migration files', { id: 'quarkus-flyway-create-button-title' })}>
                    <vaadin-icon icon="font-awesome-solid:plus"></vaadin-icon> ${msg('Create Initial Migration File', { id: 'quarkus-flyway-create-initial-migration-file' })}
                </vaadin-button>`;
        }
    }

    _nameRenderer(ds) {
        return html`${ds.name}`;
    }

    _showCreateDialog(ds){
        this._selectedDs = ds;
        this._createDialogOpened = true;
    }

    _showUpdateDialog(ds){
        this._selectedDs = ds;
        this._updateDialogOpened = true;
    }
        
    _renderCreateDialog(){    
        return html`<vaadin-dialog class="createDialog"
                    header-title=${msg('Create', { id: 'quarkus-flyway-create' })}
                    .opened="${this._createDialogOpened}"
                    @opened-changed="${(e) => (this._createDialogOpened = e.detail.value)}"
                    ${dialogRenderer(() => this._renderCreateDialogForm(), msg('Create', { id: 'quarkus-flyway-create' }))}
                ></vaadin-dialog>`;
    }

    _renderUpdateDialog(){
        return html`<vaadin-dialog class="updateDialog"
                    header-title=${msg('Update', { id: 'quarkus-flyway-update' })}
                    .opened="${this._updateDialogOpened}"
                    @opened-changed="${(e) => (this._updateDialogOpened = e.detail.value)}"
                    ${dialogRenderer(() => this._renderUpdateDialogForm(), msg('Update', { id: 'quarkus-flyway-update' }))}
                ></vaadin-dialog>`;
    }

    _renderCreateDialogForm(){
        let title = msg(str`${0} Datasource`, { id: 'quarkus-flyway-datasource-title' }, this._selectedDs.name);
        return html`<b>${title}</b></br>
            ${msg('Set up an initial file from Hibernate ORM schema generation for Flyway migrations to work.<br/>If you say yes, an initial file in <code>db/migrations</code> will be <br/>created and you can then add additional migration files as documented.', { id: 'quarkus-flyway-create-dialog-description' })}
            ${this._renderCreateDialogButtons(this._selectedDs)}
        `;
    }

    _renderUpdateDialogForm(){
        let title = msg(str`${0} Datasource`, { id: 'quarkus-flyway-datasource-title' }, this._selectedDs.name);
        return html`<b>${title}</b></br>
            ${msg('Create an incremental migration file from Hibernate ORM schema diff.<br/>If you say yes, an additional file in <code>db/migrations</code> will be <br/>created.', { id: 'quarkus-flyway-update-dialog-description' })}
            ${this._renderUpdateDialogButtons(this._selectedDs)}
        `;
    }

    _renderCreateDialogButtons(ds){
        return html`<div style="display: flex; flex-direction: row-reverse; gap: 10px;">
                        <vaadin-button theme="secondary" @click=${() => this._create(this._selectedDs)}>${msg('Create', { id: 'quarkus-flyway-create' })}</vaadin-button>
                        <vaadin-button theme="secondary error" @click=${this._cancelCreate}>${msg('Cancel', { id: 'quarkus-flyway-cancel' })}</vaadin-button>
                    </div>`;
    }

    _renderUpdateDialogButtons(ds){
        return html`<div style="display: flex; flex-direction: row-reverse; gap: 10px;">
                        <vaadin-button theme="secondary" @click=${() => this._update(this._selectedDs)}>${msg('Update', { id: 'quarkus-flyway-update' })}</vaadin-button>
                        <vaadin-button theme="secondary error" @click=${this._cancelUpdate}>${msg('Cancel', { id: 'quarkus-flyway-cancel' })}</vaadin-button>
                    </div>`;
    }

    _clean(ds) {
        if (confirm(msg('This will drop all objects (tables, views, procedures, triggers, ...) in the configured schema. Do you want to continue?', { id: 'quarkus-flyway-clean-confirm' }))) {
            this.jsonRpc.clean({ds: ds.name}).then(jsonRpcResponse => {
                this._showResultNotification(jsonRpcResponse.result);
            });
        }
    }
    
    _migrate(ds) {
        this.jsonRpc.migrate({ds: ds.name}).then(jsonRpcResponse => {
            this._showResultNotification(jsonRpcResponse.result);
            this.hotReload();
        });
    }

    _create(ds) {
        this.jsonRpc.create({ds: ds.name}).then(jsonRpcResponse => {
            this._showResultNotification(jsonRpcResponse.result);
            this._selectedDs = null;
            this._createDialogOpened = false;
            this.hotReload();
        });
    }

    _update(ds) {
        this.jsonRpc.update({ds: ds.name}).then(jsonRpcResponse => {
            this._showResultNotification(jsonRpcResponse.result);
            this._selectedDs = null;
            this._updateDialogOpened = false;
            this.hotReload();
        });
    }

    _cancelCreate(){
        this._selectedDs = null;
        this._createDialogOpened = false;
    }

    _cancelUpdate(){
        this._selectedDs = null;
        this._updateDialogOpened = false;
    }

    _showResultNotification(response){
        if(response.type === "success"){
            notifier.showInfoMessage(response.message + " (" + response.number + ")");
        }else{
            notifier.showWarningMessage(response.message);
        }
    }
    
}
customElements.define('qwc-flyway-datasources', QwcFlywayDatasources);
