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
        this._ds = null;
        this._selectedDs = null;
        this._createDialogOpened = false;
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
        return html`${this._renderCreateDialog()}
                <vaadin-grid .items="${this._ds}" class="datatable" theme="no-border">
                    <vaadin-grid-column auto-width
                                        header="Name"
                                        ${columnBodyRenderer(this._nameRenderer, [])}>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                                        header="Action"
                                        ${columnBodyRenderer(this._actionRenderer, [])}
                                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>`;
    }

    _actionRenderer(ds) {
        return html`${this._renderMigrationButtons(ds)}
                    ${this._renderCreateButton(ds)}`;
    }

    _renderMigrationButtons(ds) {
        if(ds.hasMigrations){
            let colorvar = this._cleanDisabled ? '--lumo-disabled-text-color' : '--lumo-warning-text-color';
            return html`<div id=${ds.name} style="display: inline-block;">
                <vaadin-button theme="small" @click=${() => this._clean(ds)} class="button" ?disabled=${this._cleanDisabled}>
                    <vaadin-icon style="color: var(${colorvar});" icon="font-awesome-solid:broom"></vaadin-icon> Clean
                </vaadin-button></div>
                <vaadin-button theme="small" @click=${() => this._migrate(ds)} class="button">
                    <vaadin-icon icon="font-awesome-solid:arrow-right-arrow-left"></vaadin-icon> Migrate
                </vaadin-button>
                ${this._cleanDisabled
                    ? html`<vaadin-tooltip for="${ds.name}" text="Flyway clean has been disabled via quarkus.flyway.clean-disabled=true"></vaadin-tooltip>`
                    : null}
                `;
        }
    }
    
    _renderCreateButton(ds) {
        if(ds.createPossible){
            return html`
                <vaadin-button theme="small" @click=${() => this._showCreateDialog(ds)} class="button" title="Set up basic files for Flyway migrations to work. Initial file in db/migrations will be created and you can then add additional migration files">
                    <vaadin-icon icon="font-awesome-solid:plus"></vaadin-icon> Create Initial Migration File
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
        
    _renderCreateDialog(){    
        return html`<vaadin-dialog class="createDialog"
                    header-title="Create"
                    .opened="${this._createDialogOpened}"
                    @opened-changed="${(e) => (this._createDialogOpened = e.detail.value)}"
                    ${dialogRenderer(() => this._renderCreateDialogForm(), "Create")}
                ></vaadin-dialog>`;
    }

    _renderCreateDialogForm(){
        let title = this._selectedDs.name + " Datasource";
        return html`<b>${title}</b></br>
            Set up an initial file from Hibernate ORM schema generation for Flyway migrations to work.<br/>
            If you say yes, an initial file in <code>db/migrations</code> will be <br/>
            created and you can then add additional migration files as documented. 
            ${this._renderDialogButtons(this._selectedDs)}
        `;
    }

    _renderDialogButtons(ds){
        return html`<div style="display: flex; flex-direction: row-reverse; gap: 10px;">
                        <vaadin-button theme="secondary" @click=${() => this._create(this._selectedDs)}>Create</vaadin-button>
                        <vaadin-button theme="secondary error" @click=${this._cancelCreate}>Cancel</vaadin-button>
                    </div>`;
    }

    _clean(ds) {
        if (confirm('This will drop all objects (tables, views, procedures, triggers, ...) in the configured schema. Do you want to continue?')) {
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

    _cancelCreate(){
        this._selectedDs = null;
        this._createDialogOpened = false;
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
