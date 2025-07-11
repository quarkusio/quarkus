import { LitElement, html, css} from 'lit';
import '@vaadin/progress-bar';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@qomponent/qui-code-block';
import '@vaadin/dialog';
import { dialogHeaderRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';

/**
 * This component show all available resources for MCP clients
 */
export class QwcMCPResources extends observeState(LitElement) {
    jsonRpc = new JsonRpc("resources");

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
        _resources: {state: true},
        _selectedResource: {state: true},
        _selectedResourceContent: {state: true},
        _busyReading: {state: true}
    }

    constructor() {
        super();
        this._selectedResource = [];
        this._selectedResourceContent = null;
        this._busyReading = false;
    }

    connectedCallback() {
        super.connectedCallback();
        this._loadResources();
    }

    render() {
        if (this._resources) {    
            return html`${this._renderResources()}`;
        }else{
            return this._renderProgressBar("Fetching resources...");
        }
    }

    _renderResources(){

        let dialogTitle = "";
        if(this._selectedResource.length>0)dialogTitle = this._selectedResource[0].name;

        return html`
                    <vaadin-dialog
                        header-title="Read resource: ${dialogTitle}"
                        .opened="${this._selectedResourceContent!==null}"
                        @opened-changed="${(event) => {
                            if(!event.detail.value){
                                this._selectedResource = [];
                                this._selectedResourceContent = null;
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
                        ${dialogRenderer(() => this._renderResourceContent(), this._selectedResource)}
                      ></vaadin-dialog>    
                    <vaadin-tabsheet>
                        <vaadin-tabs slot="tabs">
                            <vaadin-tab id="list-tab">List</vaadin-tab>
                            <vaadin-tab id="raw-tab">Raw json</vaadin-tab>
                        </vaadin-tabs>
                        <div tab="list-tab">
                            <vaadin-grid .items="${this._resources.resources}" .selectedItems="${this._selectedResource}" theme="row-stripes" all-rows-visible 
                                @active-item-changed="${(e) => {
                                    const item = e.detail.value;
                                    this._selectedResource = item ? [item] : [];
                                    
                                    if(this._selectedResource.length>0){
                                        this._readSelectedResourceContents();
                                    }else{
                                        this._selectedResourceContent = null;
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
                            </vaadin-grid>
                        </div>
                        <div tab="raw-tab">
                            <div class="codeBlock">
                                <qui-code-block 
                                    mode='json'
                                    content='${JSON.stringify(this._resources, null, 2)}'
                                    theme='${themeState.theme.name}'
                                    showLineNumbers>
                                </qui-code-block>
                            </div>
                    </vaadin-tabsheet>
                `;
    }

    _renderResourceContent(){
        return html`<div class="codeBlock">
                                <qui-code-block 
                                    mode='json'
                                    content='${this._selectedResourceContent}'
                                    theme='${themeState.theme.name}'
                                    showLineNumbers>
                                </qui-code-block>`;
    }

    _loadResources(){
        this.jsonRpc.list().then(jsonRpcResponse => {
            this._resources = jsonRpcResponse.result;
        });
    }

    _closeDialog(){
        this._selectedResourceContent = null;
    }

    _readSelectedResourceContents(){
        if(this._selectedResource.length>0 && !this._busyReading){

            this._busyReading = true;
            this.jsonRpc.read({uri:this._selectedResource[0].uri}).then(jsonRpcResponse => {

                if(jsonRpcResponse.result.contents.length>0){
                    let c = jsonRpcResponse.result.contents[0].text;
                    if(this._isJsonSerializable(c)){
                        this._selectedResourceContent = JSON.stringify(c, null, 2);
                    }else {
                        this._selectedResourceContent = c;
                    }
                }else{
                    this._selectedResourceContent = "No data found";
                }
                this._busyReading = false;
            });            
        }
    }

    _isJsonSerializable(value) {
      return value !== null && (typeof value === 'object');
    }

    _renderProgressBar(title){
        return html`
            <div style="color: var(--lumo-secondary-text-color);width: 95%;" >
                <div>${title}</div>
                <vaadin-progress-bar indeterminate></vaadin-progress-bar>
            </div>`;
    }

}
customElements.define('qwc-mcp-resources', QwcMCPResources);