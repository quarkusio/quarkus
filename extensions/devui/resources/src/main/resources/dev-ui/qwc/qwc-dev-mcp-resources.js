import { LitElement, html, css} from 'lit';
import '@vaadin/progress-bar';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@qomponent/qui-code-block';
import '@vaadin/dialog';
import { dialogHeaderRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import '@qomponent/qui-switch';

/**
 * This component show all available resources for MCP clients
 */
export class QwcDevMCPResources extends observeState(LitElement) {
    jsonRpc = new JsonRpc("resources");

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
                    <div class="grid">    
                        <vaadin-grid .items="${this._resources}" .selectedItems="${this._selectedResource}" theme="row-stripes no-border" all-rows-visible 
                            @active-item-changed="${(e) => {
                                const item = e.detail.value;
                                this._selectedResource = item ? [item] : [];

                                if(this._selectedResource.length>0){
                                    this._readSelectedResourceContents();
                                }else{
                                    this._selectedResourceContent = null;
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
                                header='Resource'
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
                        </vaadin-grid>
                    </div>
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
            let er = jsonRpcResponse.result.resources;
            er = (er ?? []).map(o => ({ ...o, enabled: true }));
            this.jsonRpc.listDisabled().then(jsonRpcResponse => {
                let dr = jsonRpcResponse.result.resources;
                dr = (dr ?? []).map(o => ({ ...o, enabled: false }));
                
                const collator = new Intl.Collator(undefined, { numeric: true, sensitivity: 'base' });
                
                this._resources = [...(er ?? []), ...(dr ?? [])]
                  .sort((a, b) => collator.compare(a?.name ?? '', b?.name ?? ''));
            });
        });
    }

    _closeDialog(){
        this._selectedResourceContent = null;
    }

    _namespaceRenderer(prop) {
        return html`${prop.name.split('_')[0]}`;
    }
    
    _nameRenderer(prop) {
        return html`${prop.name.split('_')[1]}`;
    }

    _stateRenderer(prop) {
        
        if(prop.enabled){  
            return html`<qui-switch size="small" checked @valueChanged=${() => this._disableTool(prop)}></qui-switch>`;
        }else{
            return html`<qui-switch size="small" @valueChanged=${() => this._enableTool(prop)}></qui-switch>`;
        }
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

    _disableTool(e){
        this.jsonRpc.disableResource({name:e.name}).then(jsonRpcResponse => {
            this._resources = this._resources.map(o =>
                o === e ? { ...o, enabled: false } : o
            );
        });
    }

    _enableTool(e){
        this.jsonRpc.enableResource({name:e.name}).then(jsonRpcResponse => {
            this._resources = this._resources.map(o =>
                o === e ? { ...o, enabled: true } : o
            );
        });
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
customElements.define('qwc-dev-mcp-resources', QwcDevMCPResources);