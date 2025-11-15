import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import '@vaadin/progress-bar';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/text-field';
import '@vaadin/button';
import '@vaadin/dialog';
import { dialogHeaderRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import '@vaadin/vertical-layout';
import 'qui-themed-code-block';
import '@qomponent/qui-badge';
import '@qomponent/qui-switch';

/**
 * This component show all available tools for MCP clients
 */
export class QwcDevMCPTools extends QwcHotReloadElement {
    jsonRpc = new JsonRpc("tools");

    static styles = css`
        .grid {
            display:flex;
            flex-direction: column;
            padding-left: 5px;
            padding-right: 5px;
            max-width: 100%;
        }

    `;

    static properties = {
        _tools: {state: true},
        _filtered: {state: true, type: Array},
        _showInputDialog: {state: true, type: Boolean},
        _selectedTool: {state: true},
        _inputvalues: { type: Object },
        _toolResult: {state: true},
        _searchTerm: {state: true}
    }

    constructor() {
        super();
        this._showInputDialog = false;
        this._selectedTool = [];
        this._inputvalues = new Map();
        this._toolResult = null;
        this._tools = null;
        this._filtered = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this._filteredValue = null;
        this._loadTools();
        this._inputvalues.clear();
    }

    render() {
        if (this._tools) {
            return html`${this._renderTools()}`;
        }else{
            return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>Fetching tools...</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>
            `;
        }
    }

    hotReload(){
        this._loadTools();
        this._inputvalues.clear();
    }

    _renderTools(){
        return html`${this._renderToolInvovationResultDialog()}
                    ${this._renderToolInvocationInputDialog()}
                    ${this._renderGrid()}`;
    }

    _renderGrid(){
        return html`<div class="grid">
                    ${this._renderFilterTextbar()}

                    <vaadin-grid .items="${this._filtered}" .selectedItems="${this._selectedTool}" theme="row-stripes no-border" all-rows-visible
                        @active-item-changed="${(e) => {
                            const item = e.detail.value;
                            this._selectedTool = item ? [item] : [];

                            if(this._selectedTool.length>0){
                                let parameters = Object.keys(this._selectedTool[0].inputSchema.properties);
                                const propertyCount = parameters.length;
                                if(propertyCount>0) {
                                    this._showInputDialog = true;
                                }else {
                                    this._testJsonRpcCall(this._selectedTool[0], null);
                                }
                            }
                        }}">
                        <vaadin-grid-column
                            header="Enabled"
                            frozen
                            auto-width
                            flex-grow="0"
                            ${columnBodyRenderer(this._stateRenderer, [])}
                        ></vaadin-grid-column>
                        <vaadin-grid-sort-column 
                            header='Namespace'
                            path="name" 
                            auto-width
                            ${columnBodyRenderer(this._namespaceRenderer, [])}
                        >
                        </vaadin-grid-sort-column>
                        <vaadin-grid-sort-column 
                            header='Method'
                            path="name" 
                            auto-width
                            ${columnBodyRenderer(this._nameRenderer, [])}
                        >
                        </vaadin-grid-sort-column>
                        <vaadin-grid-sort-column 
                            header='Description'
                            path="description" 
                            auto-width>
                        </vaadin-grid-sort-column>
                        <vaadin-grid-column
                            header="Params"
                            frozen-to-end
                            auto-width
                            flex-grow="0"
                            ${columnBodyRenderer(this._noOfParameterRenderer, [])}
                        ></vaadin-grid-column>    
                    </vaadin-grid>
                </div>`;
    }

    _renderFilterTextbar(){
        return html`<vaadin-text-field class="filterText"
                                placeholder="Filter"
                                value="${this._filteredValue}"
                                style="flex: 1;"
                                @value-changed="${(e) => this._filterTextChanged(e)}">
                            <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
                            <qui-badge slot="suffix"><span>${this._filtered?.length}</span></qui-badge>
                        </vaadin-text-field>`;
    }

    _renderToolInvovationResultDialog(){
        return html`<vaadin-dialog
                        header-title="Tool invocation result"
                        .opened="${this._toolResult!==null}"
                        @opened-changed="${(event) => {
                            if(!event.detail.value){
                                this._toolResult = null;
                            }
                        }}"
                        ${dialogHeaderRenderer(
                            () => html`
                              <vaadin-button theme="tertiary" @click="${this._closeDialog}">
                                <vaadin-icon icon="font-awesome-solid:xmark"></vaadin-icon>
                              </vaadin-button>
                            `,
                            []
                        )}
                        ${dialogRenderer(() => this._renderToolResult())}
                    ></vaadin-dialog>`;
    }

    _renderToolInvocationInputDialog(){
        return html `<vaadin-dialog
                        header-title="Input"
                        .opened="${this._showInputDialog}"
                        @opened-changed="${(event) => {
                            if(!event.detail.value){
                                this._showInputDialog = false;
                            }
                        }}"
                        ${dialogHeaderRenderer(
                            () => html`
                              <vaadin-button theme="tertiary" @click="${this._closeDialog}">
                                <vaadin-icon icon="font-awesome-solid:xmark"></vaadin-icon>
                              </vaadin-button>
                            `,
                            []
                        )}
                        ${dialogRenderer(this._renderToolInput)}
                    ></vaadin-dialog>`;
    }

    _renderToolResult(){
        return html`<div class="codeBlock">
                                <qui-themed-code-block
                                    mode='json'
                                    content='${this._toolResult}'
                                    showLineNumbers>
                                </qui-themed-code-block>`;
    }

    _renderToolInput(){
        if(this._selectedTool.length>0){
            let prop = this._selectedTool[0];
            const keys = Object.keys(prop.inputSchema.properties);
            
            return html`<vaadin-vertical-layout>
                            <b>${prop.name}</b>
                           ${keys.map(
                                (key) => html`
                                  <vaadin-text-field
                                    label="${key}"
                                    helper-text="${prop.inputSchema.properties[key].description}"
                                    placeholder="${prop.inputSchema.properties[key].type}"
                                    ?required="${(prop.inputSchema.required ?? []).includes(key)}"
                                    @input=${(e) => this._updateSelectedValue(prop.name, key, e)}
                                    @blur=${(e) => this._updateSelectedValue(prop.name, key, e)}
                                  ></vaadin-text-field>
                                `
                              )}
                              <vaadin-button @click="${() => this._getInputValuesAndTest(prop)}">Test</vaadin-button>
                        </vaadin-vertical-layout>`;
        }
    }

    _filterTextChanged(e) {
        this._searchTerm = (e.detail.value || '').trim();
        return this._filterGrid();
    }

    _filterGrid(){
        if (this._searchTerm === '') {
            this._filtered = this._tools;
            return;
        }

        this._filtered = this._tools.filter((prop) => {
           return  this._match(prop.name, this._searchTerm) || this._match(prop.description, this._searchTerm)
        });
    }

    _match(value, term) {
        if (! value) {
            return false;
        }
        return value.toLowerCase().includes(term.toLowerCase());
    }

    _closeDialog(){
        this._toolResult = null;
        this._showInputDialog = false;
    }

    _stateRenderer(prop) {
        
        if(prop.name.startsWith("tools_") || prop.name.startsWith("resources_")){
            return html``;
        }else if(prop.enabled){  
            return html`<qui-switch size="small" checked @valueChanged=${() => this._disableTool(prop)}></qui-switch>`;
        }else{
            return html`<qui-switch size="small" @valueChanged=${() => this._enableTool(prop)}></qui-switch>`;
        }
    }

    _namespaceRenderer(prop) {
        return html`${prop.name.split('_')[0]}`;
    }
    
    _nameRenderer(prop) {
        return html`${prop.name.split('_')[1]}`;
    }

    _noOfParameterRenderer(prop) {
        let parameters = Object.keys(prop.inputSchema.properties);
        const propertyCount = parameters.length;
        if(propertyCount>0) {
            return html`<qui-badge pill title="${parameters}"><span>${propertyCount}</span></qui-badge>`;
        }        
    }

    _disableTool(e){
        this.jsonRpc.disableTool({name:e.name}).then(jsonRpcResponse => {
            this._tools = this._tools.map(o =>
                o === e ? { ...o, enabled: false } : o
            );
            this._filtered = this._filtered.map(o =>
                o === e ? { ...o, enabled: false } : o
            );
        });
    }

    _enableTool(e){
        this.jsonRpc.enableTool({name:e.name}).then(jsonRpcResponse => {
            this._tools = this._tools.map(o =>
                o === e ? { ...o, enabled: true } : o
            );
            this._filtered = this._filtered.map(o =>
                o === e ? { ...o, enabled: true } : o
            );
        });
    }

    _updateSelectedValue(name, key, e){
        let params = new Map();
        if(this._inputvalues.has(name)){
            params = this._inputvalues.get(name);
        }

        params.set(key, e.target.value);

        this._inputvalues.set(name, params);

    }

    _getInputValuesAndTest(prop){
        this._showInputDialog = false;
        let params = null;
        if(this._inputvalues.has(prop.name)){
            params = this._inputvalues.get(prop.name);
            this._testJsonRpcCall(prop, params);
        }

    }

    _testJsonRpcCall(prop, params){
        const [namespace, method] = prop.name.split('_');

        let rpcClient = new JsonRpc(namespace);

        if(params){
            rpcClient[method](Object.fromEntries(params)).then(jsonRpcResponse => {
                this._setToolResult(jsonRpcResponse.result);
            });
        }else {
            rpcClient[method]().then(jsonRpcResponse => {
                this._setToolResult(jsonRpcResponse.result);
            });
        }
    }

    _setToolResult(result){
        if(this._isJsonSerializable(result)){
            this._toolResult = JSON.stringify(result, null, 2);
        }else {
            this._toolResult = result;
        }
    }

    _loadTools(){
        this.jsonRpc.list().then(jsonRpcResponse => {
            let et = jsonRpcResponse.result.tools;
            et = (et ?? []).map(o => ({ ...o, enabled: true }));
            
            this.jsonRpc.listDisabled().then(jsonRpcResponse => {
                let dt = jsonRpcResponse.result.tools;
                dt = (dt ?? []).map(o => ({ ...o, enabled: false }));
                
                const collator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });

                this._tools = [...(et ?? []), ...(dt ?? [])]
                  .sort((a, b) => collator.compare(a?.name ?? '', b?.name ?? ''));
                this._filtered = this._tools;
            });
        });
    }

    _isJsonSerializable(value) {
        return value !== null && (typeof value === 'object');
    }

}
customElements.define('qwc-dev-mcp-tools', QwcDevMCPTools);