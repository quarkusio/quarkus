import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { RouterController } from 'router-controller';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/combo-box';
import '@vaadin/item';
import '@vaadin/icon';
import '@vaadin/list-box';
import '@qomponent/qui-card';
import '@vaadin/grid';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import { columnBodyRenderer, columnHeaderRenderer } from '@vaadin/grid/lit.js';
import '@qomponent/qui-code-block';
import { notifier } from 'notifier';
import '@vaadin/progress-bar';
import '@vaadin/button';
import { devuiState } from 'devui-state';
import '@qomponent/qui-alert';

/**
 * Allows interaction with your Datasource
 */
export class QwcAgroalDatasource extends QwcHotReloadElement {
    jsonRpc = new JsonRpc(this);
    configJsonRpc = new JsonRpc("devui-configuration");
    
    routerController = new RouterController(this);
    
    static styles = css`
        .dataSources{
            display: flex;
            flex-direction: column;
            gap: 20px;
            height: 100%;
            padding-left: 10px;
        }
        .dataSourcesHeader {
            display: flex;
            align-items: baseline;
            gap: 20px;
            border-bottom-style: dotted;
            border-bottom-color: var(--lumo-contrast-10pct);
            padding-bottom: 10px;
        }
        .tablesAndData {
            display: flex;
            height: 100%;
            gap: 20px;
        }
        .tables {
        }
        .tableData {
            width: 100%;
            padding-right: 20px;
        }
    
        .tablesCard {
            min-width: 192px;
            display: flex;
        }
    
        .fill {
            width: 100%;
            height: 100%;
        }

        .pkicon{
            height: var(--lumo-icon-size-s); 
            width: var(--lumo-icon-size-s);
        }
    
        .sqlInput {
            display: flex;
            justify-content: space-between;
            gap: 10px;
        }
    
        #sql {
            width: 100%;
        }
        .data {
            display: flex;
            flex-direction: column;
            gap: 10px;
            width: 100%;
            height: 100%;
        }
    
        .sqlInputButton{
            height: var(--lumo-icon-size-s); 
            width: var(--lumo-icon-size-s);
            cursor: pointer;
            color: var(--lumo-contrast-50pct);
        }
    
        .pager {
            display: flex;
            justify-content: space-between;
        }
    
        .hidden {
            visibility: hidden;
        }
    
        .download {
            cursor: pointer;
            text-decoration: none;
            color: var(--lumo-body-text-color);
        }
    
        .download:hover {
            color: var(--lumo-primary-text-color);
            text-decoration: underline;
        }
    `;
    
    static properties = {
        _dataSources: {state: true},
        _selectedDataSource: {state: true},
        _tables: {state: true},
        _selectedTable: {state: true},
        _selectedTableIndex:{state: true},
        _selectedTableCols:{state: false},
        _currentSQL: {state: true},
        _currentDataSet: {state: true},
        _isWatching: {state: true},
        _watchId: {state: false},
        _currentPageNumber: {state: true},
        _currentNumberOfPages: {state: true},
        _allowSql: {state: true},
        _isLocalDB: {state: true},
    };
    
    constructor() {
        super();
        this._dataSources = null;
        this._selectedDataSource = null;
        this._tables = null;
        this._selectedTable = null;
        this._selectedTableCols = null;
        this._selectedTableIndex = 0;
        this._currentSQL = null;
        this._currentDataSet = null;
        this._isWatching = false;
        this._watchId = null;
        this._currentPageNumber = 1;
        this._currentNumberOfPages = 1;
        this._pageSize = 12;
        
        this._isLocalDB = false;
        var page = this.routerController.getCurrentPage();
        if(page && page.metadata){
            this._allowSql = (page.metadata.allowSql === "true");
        }else{
            this._allowSql = false;
        }
    }
    
    connectedCallback() {
        super.connectedCallback();
        
        this.jsonRpc.getDataSources().then(jsonRpcResponse => {
            this._dataSources = jsonRpcResponse.result.reduce((map, obj) => {
                    map[obj.name] = obj;
                    return map;
              }, {});
        });
    }
    
    disconnectedCallback() {
        if(this._isWatching)this._unwatch();
        super.disconnectedCallback();
    }
    
    render() {
        if(this._dataSources){
            return html`<div class="dataSources">
                            <div class="dataSourcesHeader">
                                ${this._renderDatasourcesComboBox()}
                                ${this._renderSelectedDatasource()}
                            </div>
                              ${this._renderDataOrWarning()}
                        </div>`;
        } else {
            return html`<div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Fetching data sources...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>`;
        }
    }
    
    _renderDataOrWarning(){
        if(this._isLocalDB){
            return html`<div class="tablesAndData">
                        <div class="tables">
                            ${this._renderTables()}
                        </div>
                        <div class="tableData">
                            ${this._renderDataAndDefinition()}
                        </div>
                    </div>`;
        }else{
            return html`<qui-alert level="error" permanent><span>This feature is not available for remote databases</span></qui-alert>`
        }
    }
    
    _renderDatasourcesComboBox(){
        return html`<vaadin-combo-box
                        label="Datasource"
                        item-label-path="name"
                        item-value-path="name"
                        .items="${Object.values(this._dataSources)}"
                        .value="${Object.values(this._dataSources)[0]?.name || ''}"
                        @value-changed="${this._onDataSourceChanged}"
                        .allowCustomValue="${false}"
                    ></vaadin-combo-box>
                `;
    }
    
    _renderSelectedDatasource(){
        if(this._selectedDataSource){
            return html`<code>${this._selectedDataSource.jdbcUrl}</code>`;
        }
    }
    
    _renderTables(){
        if(this._tables){
            return html`<qui-card class="tablesCard" header="Tables">
                            <div slot="content">
                                <vaadin-list-box selected="0" @selected-changed="${this._onTableChanged}">
                                    ${this._tables.map((table) =>
                                        html`<vaadin-item>${table.tableName}</vaadin-item>`
                                    )}
                                </vaadin-list-box>
                            </div>
                        </qui-card>`;
        }else{
            return html`<div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Fetching tables...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>`;
        }
        
    }
    
    _renderDataAndDefinition(){
        return html`<vaadin-tabsheet class="fill" theme="bordered">
                        <vaadin-button slot="suffix" theme="icon" title="Refresh" aria-label="Refresh">
                            <vaadin-icon @click=${this.hotReload} icon="font-awesome-solid:arrows-rotate"></vaadin-icon>
                        </vaadin-button>
                        
                        ${this._renderWatchButton()}
                        
                        <vaadin-tabs slot="tabs">
                          <vaadin-tab id="data-tab">Data</vaadin-tab>
                          <vaadin-tab id="definition-tab">Definition</vaadin-tab>
                        </vaadin-tabs>

                        <div tab="data-tab" style="height:100%;">${this._renderTableData()}</div>
                        <div tab="definition-tab" style="height:100%;">${this._renderTableDefinition()}</div>
                      </vaadin-tabsheet>`;
    }
    
    _renderWatchButton(){
        if(this._isWatching){
            return html`<vaadin-button slot="suffix" theme="icon" title="Stop watching" aria-label="Stop watching">
                            <vaadin-icon @click=${this._unwatch} icon="font-awesome-solid:eye"></vaadin-icon>
                        </vaadin-button>`;
        }else{
            return html`<vaadin-button slot="suffix" theme="icon" title="Start watching" aria-label="Start watching">
                            <vaadin-icon @click=${this._watch} icon="font-awesome-solid:eye-slash"></vaadin-icon>
                        </vaadin-button>`;
        }
    }
    
    _renderTableData(){
        if(this._selectedTable && this._currentDataSet && this._currentDataSet.cols){
            return html`<div class="data">
                            <vaadin-grid .items="${this._currentDataSet.data}" theme="row-stripes no-border" class="fill" column-reordering-allowed>
                                ${this._currentDataSet.cols.map((col) => 
                                    this._renderTableHeader(col)
                                )}
                                <span slot="empty-state">No data.</span>
                            </vaadin-grid>
                            ${this._renderPager()}
                            ${this._renderSqlInput()}
                        </div>    
                    `;
        }else{
            return html`<div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Fetching data...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>`;
        }
    }
    
    _renderTableHeader(col){
        let heading = col;
        if(this._selectedTable.primaryKeys.includes(col)){
            heading = col + " *";
        }
        return html`<vaadin-grid-sort-column path="${col}" header="${heading}" auto-width resizable ${columnBodyRenderer(
            (item) => this._cellRenderer(col, item),
            []
          )}></vaadin-grid-sort-column>`;
    }
    
    _renderTableDefinition(){
        if(this._selectedTable){
            return html`<vaadin-grid .items="${this._selectedTable.columns}" theme="row-stripes no-border" class="fill" column-reordering-allowed>
                            <vaadin-grid-sort-column path="columnName" auto-width resizable ${columnBodyRenderer(this._columnNameRenderer, [])}></vaadin-grid-sort-column>
                            <vaadin-grid-sort-column path="columnType" auto-width resizable></vaadin-grid-sort-column>
                            <vaadin-grid-sort-column path="columnSize" auto-width resizable></vaadin-grid-sort-column>
                            <vaadin-grid-sort-column path="nullable" auto-width resizable></vaadin-grid-sort-column>
                            <vaadin-grid-sort-column path="binary" auto-width resizable></vaadin-grid-sort-column>
                        </vaadin-grid>`;
        }
    }
    _renderPager() {    
        return html`<div class="pager">
                        ${this._renderPreviousPageButton()}
                        <span>${this._currentPageNumber} of ${this._currentNumberOfPages}</span>
                        ${this._renderNextPageButton()}
                    </div>`;
    }
    
    _renderPreviousPageButton(){
        let klas = "pageButton";
        if(this._currentPageNumber === 1){
            klas = "hidden";
        }
        return html`<vaadin-button theme="icon tertiary" aria-label="Previous" @click=${this._previousPage} class="${klas}">
                        <vaadin-icon icon="font-awesome-solid:circle-chevron-left"></vaadin-icon>
                    </vaadin-button>`;
    }
    
    _renderNextPageButton(){
        let klas = "pageButton";
        if(this._currentPageNumber === this._currentNumberOfPages){
            klas = "hidden";
        }
        return html`<vaadin-button theme="icon tertiary" aria-label="Next" @click=${this._nextPage} class="${klas}">
                        <vaadin-icon icon="font-awesome-solid:circle-chevron-right"></vaadin-icon>
                    </vaadin-button>`;
    }
    
    _renderSqlInput(){
        if(this._allowSql){
            return html`<div class="sqlInput">
                        <qui-code-block @shiftEnter=${this._shiftEnterPressed} content="${this._currentSQL}" id="sql" mode="sql" theme="dark" value='${this._currentSQL}' editable></qui-code-block>
                        <vaadin-icon class="sqlInputButton" title="Clear" icon="font-awesome-solid:broom" @click=${this._clearSqlInput}></vaadin-icon>
                        <vaadin-icon class="sqlInputButton" title="Run" icon="font-awesome-solid:person-running" @click=${this._executeClicked}></vaadin-icon>
                    </div>`;
        }else {
            return html`<vaadin-button theme="small" @click="${this._handleAllowSqlChange}">Allow any SQL execution from here</vaadin-button>`;
        }
    }
    
    _handleAllowSqlChange(){
        this.configJsonRpc.updateProperty({
            'name': '%dev.quarkus.datasource.dev-ui.allow-sql',
            'value': 'true'
        }).then(e => {
            this._allowSql = true;
        });;
    }
    
    _columnNameRenderer(col){
        if(this._selectedTable.primaryKeys.includes(col.columnName)){
            return html`${col.columnName} <vaadin-icon class="pkicon" icon="font-awesome-solid:key"></vaadin-icon>`;
        }else{
            return html`${col.columnName}`;
        }
    }
    
    _cellRenderer(columnName, item){
        const value = item[columnName];
        if(value){
            let colDef = this._selectedTableCols.get(columnName);
        
            if(colDef.binary){
                const byteCharacters = atob(value);
                const byteNumbers = new Array(byteCharacters.length);
                for (let i = 0; i < byteCharacters.length; i++) {
                    byteNumbers[i] = byteCharacters.charCodeAt(i);
                }
                const byteArray = new Uint8Array(byteNumbers);

                const blob = new Blob([byteArray], { type: 'application/octet-stream' });
                const url = URL.createObjectURL(blob);
                
                return html`<a class="download" href="${url}" download="download">download</span>`;
            }else{
                return html`<span>${value}</span>`;
            }
        }
    }
    
    _watch(){
        this._isWatching = true;
        this._watchId = setInterval(() => {
            this.hotReload();
        }, 3000);
    }
    
    _unwatch(){
        this._isWatching = false;
        clearInterval(this._watchId);
        this._watchId = null;
    }
    
    _onDataSourceChanged(event) {
        const selectedValue = event.detail.value;
        if(selectedValue in this._dataSources){
            this._selectedDataSource = this._dataSources[selectedValue];
            this._isLocalDB = this._isLocalhostDatabase();
            if(this._isLocalDB){
                this._fetchTableDefinitions();
            }
        }
    }
    
    _onTableChanged(event){
        this._selectedTableIndex = event.detail.value;
        this._selectedTable = this._tables[this._selectedTableIndex];
        this._clearSqlInput();
    }
    
    _previousPage(){
        if(this._currentPageNumber!=1){
            this._currentPageNumber = this._currentPageNumber - 1;
            this._executeCurrentSQL();
        }
    }
    
    _nextPage(){
        this._currentPageNumber = this._currentPageNumber + 1;
        this._executeCurrentSQL();
    }
    
    _getNumberOfPages(){
        if(this._currentDataSet){
            if(this._currentDataSet.totalNumberOfElements > this._pageSize){
                return Math.ceil(this._currentDataSet.totalNumberOfElements/this._pageSize);
            }else {
                return 1;
            }
        }
    }
    
    _executeCurrentSQL(){
        if(this._currentSQL){
            this.jsonRpc.executeSQL({
                                    datasource:this._selectedDataSource.name, 
                                    sql:this._currentSQL,
                                    pageNumber: this._currentPageNumber,
                                    pageSize: this._pageSize
                                }).then(jsonRpcResponse => {
                if(jsonRpcResponse.result.error){
                    notifier.showErrorMessage(jsonRpcResponse.result.error);
                } else if (jsonRpcResponse.result.message){
                    notifier.showMessage(jsonRpcResponse.result.message);
                    this._clearSqlInput();
                } else {
                    this._currentDataSet = jsonRpcResponse.result;
                    this._currentNumberOfPages = this._getNumberOfPages();
                }
            });
        }
    }
    
    _fetchTableDefinitions() {
        if(this._selectedDataSource){
            this.jsonRpc.getTables({datasource:this._selectedDataSource.name}).then(jsonRpcResponse => {
                this._tables = jsonRpcResponse.result;
                this._selectedTable = this._tables[this._selectedTableIndex];
                this._selectedTableCols = this._selectedTable.columns.reduce((acc, obj) => {
                    acc.set(obj.columnName, obj);
                    return acc;
                  }, new Map());
                
                this._executeCurrentSQL();
            });
        }
    }
    
    _executeClicked(){
        let newValue = this.shadowRoot.getElementById('sql').getAttribute('value');
        this._executeSQL(newValue);
    }
    
    _clearSqlInput(){
        this._executeSQL("select * from " + this._selectedTable.tableName);
    }
    
    _shiftEnterPressed(event){
        this._executeSQL(event.detail.content);
    }
    
    _executeSQL(sql){
        this._currentSQL = sql;
        this._executeCurrentSQL();
    }
    
    _startsWithIgnoreCaseAndSpaces(str, searchString) {
        return str.trim().toLowerCase().startsWith(searchString.toLowerCase());
    }
    
    hotReload(){
        this._fetchTableDefinitions();
    }
    
    _isLocalhostDatabase() {
        
        let jdbcUrl = this._selectedDataSource.jdbcUrl;
        try {
            if (jdbcUrl.startsWith("jdbc:h2:mem:") || jdbcUrl.startsWith("jdbc:h2:file:")) {
                return true;
            }

            if (jdbcUrl.startsWith("jdbc:derby:memory:")) {
                return true;
            }

            if (jdbcUrl.startsWith("jdbc:derby:")) {
                const derbyUri = jdbcUrl.replace("jdbc:", "");
                if (derbyUri.startsWith("localhost") || derbyUri.startsWith("127.0.0.1")) {
                    return true;
                }
            }

            const urlPattern = /^jdbc:[^:]+:\/\/([^:/]+)(:\d+)?/;
            const match = jdbcUrl.match(urlPattern);

            if (match) {
                const host = match[1];
                return host === "localhost" || host === "127.0.0.1" || host === "::1";
            }

            return false;
        } catch (e) {
            console.error(e);
            return false;
        }
    }
}
customElements.define('qwc-agroal-datasource', QwcAgroalDatasource);
