import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/button';
import '@vaadin/split-layout';
import '@vaadin/menu-bar';
import '@vaadin/tooltip';
import '@qomponent/qui-code-block';
import '@qomponent/qui-directory-tree';
import '@qomponent/qui-badge';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/dialog';
import '@vaadin/progress-bar';
import MarkdownIt from 'markdown-it';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { dialogFooterRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import { notifier } from 'notifier';

/**
 * This component shows the workspace
 */
export class QwcWorkspace extends observeState(QwcHotReloadElement) { 
    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            height: 100%;
        }
        
        .codeBlock {
            width: 100%;
            height: 100%;
        }
    
        .split {
            display: flex;
            width: 100%;
            height: 100%;
        }
    
        .split vaadin-split-layout {
            width: 100%;
        }
    
        .files {
            height: 100%;
            display: flex;
            padding-left: 10px;
        }
        .nothing {
            display: flex;
        }
    
        .actionSplitScreen {
            display: flex;
            flex-direction: column;
        }
    
        .actionButtonBar {
            display: flex;
            flex-direction: row-reverse;
            padding-right: 10px;
        }
    `;
    
    static properties = {
        _workspaceItems: {state: true},
        _workspaceTreeNames: {state: true},
        _workspaceActions: {state: true},
        _filteredActions: {state: true},
        _selectedWorkspaceItem: {state: true},
        _actionResultContent: {state:true},
        _actionResultDisplay: {state:true},
        _actionResultDisplayType: {state:true},
        _showActionProgress: {state: true}
    };

    constructor() { 
        super();
        this.md = new MarkdownIt();
        this._workspaceItems = null;
        this._workspaceTreeNames = null;
        this._workspaceActions = [];
        this._filteredActions = this._workspaceActions;
        this._clearSelectedWorkspaceItem();
        this._clearActionResult();
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload(){
        this._workspaceItems = null;
        this._workspaceActions = [];
        this._filteredActions = this._workspaceActions;
        this._clearActionResult();
        
        this._loadWorkspaceItems();
        this._loadWorkspaceActions();
    }

    disconnectedCallback() {
        super.disconnectedCallback();      
    }

    updated(changedProperties) {
        if (changedProperties.has('_workspaceItems')) {
            if(this._workspaceItems && this._workspaceItems.size>0){
                this.afterRender();
            }
        }
    }

    async afterRender() {
        await this.updateComplete;
        if(this._workspaceItems && this._workspaceItems.size>0){
            
            let directoryTree = this.shadowRoot.getElementById('directoryTree');
            
            // TODO: The context menu on directory tree is not rendering in the correct place. Fix that first            
            //            directoryTree.contextMenuItems = [
            //                {
            //                  title: 'Delete',
            //                  callback: (filePath, node) => console.log(`Deleting ${filePath}`)
            //                }
            //              ];
            
            if(this._selectedWorkspaceItem.name){
                directoryTree.selectFile(this._selectedWorkspaceItem.name);
                this._selectWorkspaceItem(this._selectedWorkspaceItem);
            } else {
                directoryTree.selectFile([...this._workspaceItems.values()][0].name);
                this._selectWorkspaceItem([...this._workspaceItems.values()][0]);
            }
        }
    }

    render() { 
        if (this._workspaceItems) {
            return html`<div class="split">
                            <vaadin-split-layout>
                                <master-content style="width: 25%;">${this._renderWorkspaceTree()}</master-content>
                                <detail-content style="width: 75%;">${this._renderSelectedSource()}</detail-content>
                            </vaadin-split-layout>
                        </div>
                        ${this._renderResultDialog()}`;
        } else {
            return html`<div class="nothing">No code found. <span class="checkNow" @click="${this.hotReload}">Check now</span></div>`;
        }
    }
    
    _renderWorkspaceTree(){
        return html`<qui-directory-tree id="directoryTree" class="files"
                        .directory="${this._workspaceTreeNames}"
                        header="Source Code"
                        @file-select="${this._onFileSelect}"
                    ></qui-directory-tree>`;
    }

    _renderSelectedSource(){
        if(this._selectedWorkspaceItem.name){
            return html`<vaadin-split-layout>
                            <master-content style="width: 50%;">
                                <vaadin-tabsheet>

                                    <vaadin-button slot="prefix" theme="icon" aria-label="Save" @click="${this._saveSelectedWorkspaceItem}">
                                      <vaadin-icon icon="font-awesome-solid:floppy-disk"></vaadin-icon>
                                    </vaadin-button>
                                    <vaadin-button slot="prefix" theme="icon" aria-label="Copy" @click="${this._copySelectedWorkspaceItem}">
                                      <vaadin-icon icon="font-awesome-solid:copy"></vaadin-icon>
                                    </vaadin-button>

                                    ${this._renderActions()}

                                    <vaadin-tabs slot="tabs">
                                      <vaadin-tab id="${this._selectedWorkspaceItem.path}" title="${this._selectedWorkspaceItem.path}">${this._selectedWorkspaceItem.name.split('/').pop()}</vaadin-tab>
                                    </vaadin-tabs>

                                    <div tab="${this._selectedWorkspaceItem.path}">
                                        ${this._renderContent()}
                                    </div>
                                </vaadin-tabsheet>
                            </master-content>
                            ${this._renderResultSplitView()}
                        </vaadin-split-layout>`;
        }
    }
    
    _renderResultSplitView(){
        if(this._actionResultContent && this._actionResultDisplay === "split"){
            return html`<detail-content style="width: 50%;">
                <div class="actionSplitScreen">
                    <div class="actionButtonBar">
                        <vaadin-button theme="icon" aria-label="Close" @click="${this._clearActionResult}">
                            <vaadin-icon icon="font-awesome-solid:xmark"></vaadin-icon>
                            <vaadin-tooltip slot="tooltip" text="Close"></vaadin-tooltip>
                        </vaadin-button>
                    </div>
                    ${this._renderActionResult()}
                </div>
            </detail-content>`;
        }
    }
    
    _renderResultDialog(){
        if(this._actionResultContent && this._actionResultDisplay === "dialog"){
            return html`<vaadin-dialog
                            resizable
                            draggable
                            .opened=true
                            ${dialogRenderer(this._renderActionResult, [])}
                        ></vaadin-dialog>`;
        }
    }
    
    _renderActionResult(){
        if(this._actionResultContent && this._actionResultDisplayType === "raw"){
            return html`${this._actionResultContent}`;
        }else if(this._actionResultContent && this._actionResultDisplayType === "code"){
            // TODO: We can not assume the mode is the same as the input
            // Maybe return name|content ?
            return html`<qui-code-block id="code" class='codeBlock'
                                    mode='${this._getMode(this._selectedWorkspaceItem.name)}' 
                                    theme='${themeState.theme.name}'
                                    .content='${this._actionResultContent}'
                                    showLineNumbers>
                                </qui-code-block>`;
        }else if(this._actionResultContent && this._actionResultDisplayType === "markdown"){
            const htmlContent = this.md.render(this._actionResultContent);
            return html`${unsafeHTML(htmlContent)}`; 
        }else if(this._actionResultContent && this._actionResultDisplayType === "html"){
            return html`${unsafeHTML(this._actionResultContent)}`; 
        }else if(this._actionResultContent && this._actionResultDisplayType === "image"){
            let imgurl = `data:image/png;base64,${this._actionResultContent}`
            return html`<img src="${imgurl}" alt="${this._selectedWorkspaceItem.name}" />`;
        }
    }
    
    _renderActions(){
        if(this._filteredActions){
            if(this._showActionProgress){
                return html`<vaadin-progress-bar slot="suffix" indeterminate></vaadin-progress-bar>`;
            }else{
                return html`<div class="actions" slot="suffix">
                            <vaadin-menu-bar .items="${this._filteredActions}" theme="dropdown-indicators" @item-selected="${(e) => this._actionSelected(e)}"></vaadin-menu-bar>
                        </div>`;
            }
        }
    }
    
    _renderContent(){
        if(this._selectedWorkspaceItem.type.startsWith("image")){
            let imgurl = `data:image/png;base64,${this._selectedWorkspaceItem.content}`
            return html`<img src="${imgurl}" alt="${this._selectedWorkspaceItem.name}" />`;
        }else{
            return html`<qui-code-block id="code" class='codeBlock' @keydown="${this._onKeyDown}"
                                    mode='${this._getMode(this._selectedWorkspaceItem.name)}'
                                    theme='${themeState.theme.name}'
                                    .content='${this._selectedWorkspaceItem.content}'
                                    value='${this._selectedWorkspaceItem.content}'
                                    showLineNumbers
                                    editable>
                                </qui-code-block>`;
        }
        // TODO: Allow other non-text files (example zip)
    }
    
    _actionSelected(e){
        this._showActionProgress = true;
        this._clearActionResult();
        let actionId = e.detail.value.id;
        
        let newWorkspaceItemValue = this._selectedWorkspaceItem.content;
        let codeElement = this.shadowRoot.getElementById('code');
        if(codeElement){
            newWorkspaceItemValue = codeElement.getAttribute('value');
        }
        
        this.jsonRpc.executeAction({actionId:actionId,
                                    name:this._selectedWorkspaceItem.name,
                                    path:this._selectedWorkspaceItem.path,
                                    content:newWorkspaceItemValue,
                                    type:this._selectedWorkspaceItem.type}).then(jsonRpcResponse => { 
            if(e.detail.value.display === "notification"){
                notifier.showInfoMessage(jsonRpcResponse.result.result);
            }else if(e.detail.value.display === "replace"){
                // TODO: This does not take Markdown into context....
                // TODO: Use result
                this._selectedWorkspaceItem.content = jsonRpcResponse.result.result;
                this._selectedWorkspaceItem.type = e.detail.value.displayType;
                this._selectedWorkspaceItem.path
            }else if(e.detail.value.display !== "nothing"){
                this._actionResultContent = jsonRpcResponse.result.result;
                this._actionResultDisplay = e.detail.value.display;
                this._actionResultDisplayType = e.detail.value.displayType;
            }
            this._showActionProgress = false;
        });
    }
    
    _filterActions(name) {
        this._filteredActions = this._workspaceActions.map(actionGroup => {
        
            const filteredChildren = actionGroup.children.filter(child => {
                if(child.pattern){
                    const regex = new RegExp(child.pattern);
                    return regex.test(name);
                }
                return true;
            });

            if (filteredChildren.length > 0) {
                return { ...actionGroup, children: filteredChildren };
            }
            return null;
        }).filter(actionGroup => actionGroup !== null);
    }
    
    _getMode(fileName) {
        const parts = fileName.split('.');
        if (parts.length > 1) {
            return parts.pop().toLowerCase();
        }
        return null;
    }

    _onKeyDown(event) {
        if ((event.ctrlKey || event.metaKey) && event.key === 's') {
          event.preventDefault();
          this._saveSelectedWorkspaceItem();
        }
    }

    _saveSelectedWorkspaceItem(){
        let codeElement = this.shadowRoot.getElementById('code');
        if(codeElement){
            let newWorkspaceItemValue = codeElement.getAttribute('value');
        
            if(newWorkspaceItemValue!=this._selectedWorkspaceItem.content){
                this.jsonRpc.saveWorkspaceItemContent({content:newWorkspaceItemValue, path:this._selectedWorkspaceItem.path}).then(jsonRpcResponse => { 
                    if(jsonRpcResponse.result.success){
                        notifier.showInfoMessage(jsonRpcResponse.result.path + " saved successfully");
                        this._selectedWorkspaceItem = { ...this._selectedWorkspaceItem, content: newWorkspaceItemValue, isDirty: false };
                        super.forceRestart();
                    }else {
                        notifier.showErrorMessage(jsonRpcResponse.result.path + " NOT saved. " + jsonRpcResponse.result.errorMessage);
                    }
                });
            }
        }
    }
    
    _copySelectedWorkspaceItem(){
        let content = this._selectedWorkspaceItem.content;
    
        let codeElement = this.shadowRoot.getElementById('code');
        if(codeElement){
            content = this.shadowRoot.getElementById('code').getAttribute('value');
        }
        
        const path = this._selectedWorkspaceItem?.path;
        if (!content) {
            notifier.showWarningMessage(path + " has no content");
            return;
        }
        
        navigator.clipboard.writeText(content)
            .then(() => {
                notifier.showInfoMessage("Content copied to clipboard");
            })
            .catch(err => {
                notifier.showErrorMessage("Failed to copy content:" + err);
            });
    }
    
    _onFileSelect(event) {
        this._clearSelectedWorkspaceItem();
        this._selectWorkspaceItem(this._workspaceItems.get(event.detail.file));
    }
    
    _clearSelectedWorkspaceItem(){
        this._selectedWorkspaceItem = { name: null, path: null, content: null, isDirty: false };
    }
    
    _clearActionResult(){
        this._actionResultContent = null;
        this._actionResultDisplay = null;
        this._actionResultDisplayType = null;
        this._showActionProgress = false;
    }
    
    _selectWorkspaceItem(workspaceItem){
        this.jsonRpc.getWorkspaceItemContent({path:workspaceItem.path}).then(jsonRpcResponse => {
            this._selectedWorkspaceItem = { ...this._selectedWorkspaceItem, 
                type: jsonRpcResponse.result.type,
                content: jsonRpcResponse.result.content,
                name:  workspaceItem.name,
                path:  workspaceItem.path
            };            
        });
        this._filterActions(workspaceItem.name);
        
        this._clearActionResult();
    }
    
    _loadWorkspaceItems(){
        this.jsonRpc.getWorkspaceItems().then(jsonRpcResponse => {
            if (Array.isArray(jsonRpcResponse.result)) {
                this._workspaceItems = new Map(jsonRpcResponse.result.map(obj => [obj.name, obj]));                
            } else {
                console.error("Expected an array but got:", jsonRpcResponse.result);
            }
            this._workspaceTreeNames = this._convertDirectoryStructureToTree();
        });
    }
    
    _loadWorkspaceActions(){
        this.jsonRpc.getWorkspaceActions().then(jsonRpcResponse => {
            this._workspaceActions = [
                {
                  text: "Action", className: 'bg-primary text-primary-contrast', 
                  children: jsonRpcResponse.result.map(item => (
                            { 
                                text: item.label, 
                                id: item.id, 
                                pattern: item.pattern,
                                display: item.display,
                                displayType: item.displayType
                            }
                        )
                    )
                }
            ];
        });
    }
    
    _filterActions(name) {
        this._filteredActions = this._workspaceActions.map(actionGroup => {
        
            const filteredChildren = actionGroup.children.filter(child => {
                if(child.pattern){
                    const regex = new RegExp(child.pattern);
                    return regex.test(name);
                }
                return true;
            });

            if (filteredChildren.length > 0) {
                return { ...actionGroup, children: filteredChildren };
            }
            return null;
        }).filter(actionGroup => actionGroup !== null);
    }

    _convertDirectoryStructureToTree() {
        const root = [];
        this._workspaceItems.forEach((value, key) => {
            const parts = value.name.split('/');
            let currentLevel = root;

            parts.forEach((part, index) => {
                const isFile = index === parts.length - 1;
                let existing = currentLevel.find((item) => item.name === part);

                if (existing) {
                    currentLevel = existing.children;
                } else {
                    const newItem = {
                        name: part,
                        type: isFile ? 'file' : 'folder',
                        children: isFile ? null : [],
                        ...(isFile && { path: value.path }) 
                    };
              
                    currentLevel.push(newItem);
                    currentLevel = isFile ? null : newItem.children;
                }
            });
        });

        return root;
    }
}
customElements.define('qwc-workspace', QwcWorkspace);