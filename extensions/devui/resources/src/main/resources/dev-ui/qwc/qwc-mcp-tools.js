import { LitElement, html, css} from 'lit';
import '@vaadin/progress-bar';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/text-field';
import '@vaadin/button';
import '@vaadin/dialog';
import { dialogHeaderRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import '@vaadin/vertical-layout';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@qomponent/qui-code-block';
import '@qomponent/qui-badge';

/**
 * This component show all available tools for MCP clients
 */
export class QwcMCPTools extends observeState(LitElement) {
    jsonRpc = new JsonRpc("tools");

    static styles = css`
        :host {
            height: 100%;
            display:flex;
            width: 100%;
        }
    
        vaadin-tabsheet {
            width: 100%;
            height: 100%;
        }
    
    `;

    static properties = {
        _tools: {state: true},
        _showInputDialog: {state: true, type: Boolean},
        _selectedTool: {state: true},
        _inputvalues: { type: Object },
        _toolResult: {state: true}
    }

    constructor() {
        super();
        this._showInputDialog = false;
        this._selectedTool = [];
        this._inputvalues = new Map();
        this._toolResult = null;
    }

    connectedCallback() {
        super.connectedCallback();
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

    _renderTools(){
        return html`
                    <vaadin-dialog
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
                    ></vaadin-dialog>  
                    
                    <vaadin-dialog
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
                    ></vaadin-dialog>
                    
                    <vaadin-tabsheet>
                        <vaadin-tabs slot="tabs">
                            <vaadin-tab id="list-tab">List</vaadin-tab>
                            <vaadin-tab id="raw-tab">Raw json</vaadin-tab>
                        </vaadin-tabs>
                        <div tab="list-tab">
                            <vaadin-grid .items="${this._tools.tools}" .selectedItems="${this._selectedTool}" theme="row-stripes" all-rows-visible
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
                                <vaadin-grid-sort-column 
                                    header='Name'
                                    path="name" 
                                    auto-width>
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
                        </div>
                        <div tab="raw-tab">
                            <div class="codeBlock">
                                <qui-code-block 
                                    mode='json'
                                    content='${JSON.stringify(this._tools, null, 2)}'
                                    theme='${themeState.theme.name}'
                                    showLineNumbers>
                                </qui-code-block>
                            </div>
                    </vaadin-tabsheet>
                `;
    }

    _renderToolResult(){
        return html`<div class="codeBlock">
                                <qui-code-block 
                                    mode='json'
                                    content='${this._toolResult}'
                                    theme='${themeState.theme.name}'
                                    showLineNumbers>
                                </qui-code-block>`;
    }

    _renderToolInput(){
        if(this._selectedTool.length>0){
            let prop = this._selectedTool[0];

            const keys = Object.keys(prop.inputSchema.properties);

            return html`<vaadin-vertical-layout>
                           ${keys.map(
                                (key) => html`
                                  <vaadin-text-field
                                    label="${key}"
                                    @input=${(e) => this._updateSelectedValue(prop.name, key, e)}
                                    @blur=${(e) => this._updateSelectedValue(prop.name, key, e)}
                                  ></vaadin-text-field>
                                `
                              )}
                              <vaadin-button @click="${() => this._getInputValuesAndTest(prop)}">Test</vaadin-button>
                        </vaadin-vertical-layout>`;
        }
    }

    _closeDialog(){
        this._toolResult = null;
        this._showInputDialog = false;
    }

    _noOfParameterRenderer(prop) {
        let parameters = Object.keys(prop.inputSchema.properties);
        const propertyCount = parameters.length;
        if(propertyCount>0) {
            return html`<qui-badge pill title="${parameters}"><span>${propertyCount}</span></qui-badge>`;
        }        
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
        const [namespace, method] = prop.name.split('/');

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
            this._tools = jsonRpcResponse.result;
        });
    }

    _isJsonSerializable(value) {
        return value !== null && (typeof value === 'object');
    }

}
customElements.define('qwc-mcp-tools', QwcMCPTools);