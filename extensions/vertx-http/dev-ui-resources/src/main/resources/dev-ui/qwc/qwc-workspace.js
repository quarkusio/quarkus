import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import { RouterController } from 'router-controller';
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
import '@vaadin/confirm-dialog';
import '@vaadin/progress-bar';
import MarkdownIt from 'markdown-it';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { dialogHeaderRenderer, dialogFooterRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import { notifier } from 'notifier';
import './qwc-workspace-binary.js';
import 'qui-ide-link';

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
    
        iframe {
            width: 100%;
            height: 100vh;
            border: none;
        }
    `;
    
    static properties = {
        _workspaceItems: {state: true},
        _workspaceTreeNames: {state: true},
        _workspaceActions: {state: true},
        _filteredActions: {state: true},
        _selectedWorkspaceItem: {state: true},
        _changeToWorkspaceItem: {state: true},
        _actionResult: {state: true},
        _showActionProgress: {state: true},
        _confirmDialogOpened: {state: true}
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
        this._confirmDialogOpened = false;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
        RouterController.registerGuardedComponent(this); // We want to confirm if there is changes and someone navigate away
        
        this._beforeUnloadHandler = (e) => {
            if (this.shouldConfirmAwayNavigation()) {
                e.preventDefault();
                e.returnValue = 'You have unsaved changes. Are you sure you want to leave?';
                return e.returnValue;
            }
        };
        window.addEventListener('beforeunload', this._beforeUnloadHandler);
    }

    hotReload(){
        this._workspaceItems = null;
        this._workspaceActions = [];
        this._filteredActions = this._workspaceActions;
        this._clearActionResult();
        this._loadWorkspaceActions();
        this._loadWorkspaceItems();
    }

    disconnectedCallback() {
        RouterController.unregisterGuardedComponent(this);
        window.removeEventListener('beforeunload', this._beforeUnloadHandler);
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
                        ${this._renderResultDialog()}
                        ${this._renderConfirmDialog()}`;
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

                                    <vaadin-button title="Save" slot="prefix" theme="icon" aria-label="Save" @click="${this._saveSelectedWorkspaceItem}">
                                      <vaadin-icon icon="font-awesome-solid:floppy-disk"></vaadin-icon>
                                    </vaadin-button>
                                    <vaadin-button title="Copy" slot="prefix" theme="icon" aria-label="Copy" @click="${this._copySelectedWorkspaceItem}">
                                      <vaadin-icon icon="font-awesome-solid:copy"></vaadin-icon>
                                    </vaadin-button>
                                    
            
                                    ${this._renderActions()}

                                    <qui-ide-link slot="suffix" title="Open in IDE" style="cursor: pointer;"
                                        fileName="${this._selectedWorkspaceItem.path}"
                                        lineNumber="0"
                                        noCheck>
                                        <vaadin-icon icon="font-awesome-solid:up-right-from-square"></vaadin-icon>      
                                    </qui-ide-link>

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
        if(this._actionResult && this._actionResult.content && this._actionResult.display === "split"){
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
        if(this._actionResult && this._actionResult.content && this._actionResult.display === "dialog"){
            return html`<vaadin-dialog style="min-width=50vw;"
                            header-title="${this._actionResult?.name ?? this._actionResult?.path}"
                            resizable
                            draggable
                            .opened=${true}
                            ${dialogHeaderRenderer(
                                () => html`
                                <vaadin-button theme="tertiary" @click="${this._clearActionResult}">
                                  <vaadin-icon icon="font-awesome-solid:xmark"></vaadin-icon>
                                </vaadin-button>
                              `,
                              []
                            )}
                            ${dialogRenderer(this._renderActionResult, [])}
                            ${dialogFooterRenderer(
                            () => html`
                                <vaadin-button theme="primary" @click="${this._saveActionResult}">
                                    Save
                                </vaadin-button>
                                <vaadin-button theme="tertiary" @click="${this._copyActionResult}">Copy</vaadin-button>
                                `,
                                []
                            )}
                        ></vaadin-dialog>`;
        }
    }
    
    _renderActionResult(){
        if(this._actionResult && this._actionResult.content && this._actionResult.displayType === "raw"){
            return html`${this._actionResult.content}`;
        }else if(this._actionResult && this._actionResult.content && this._actionResult.displayType === "code"){
            // TODO: We can not assume the mode is the same as the input
            // Maybe return name|content ?
            return html`<qui-code-block id="code" class='codeBlock'
                                    mode='${this._getMode(this._actionResult?.name ?? this._actionResult?.path)}' 
                                    theme='${themeState.theme.name}'
                                    .content='${this._actionResult.content}'
                                    showLineNumbers>
                                </qui-code-block>`;
        }else if(this._actionResult && this._actionResult.content && this._actionResult.displayType === "markdown"){
            const htmlContent = this.md.render(this._actionResult.content);
            return html`${unsafeHTML(htmlContent)}`; 
        }else if(this._actionResult && this._actionResult.content && this._actionResult.displayType === "html"){
            return html`${unsafeHTML(this._actionResult.content)}`; 
        }else if(this._actionResult && this._actionResult.content && this._actionResult.displayType === "image"){
            let imgurl = `data:image/png;base64,${this._actionResult.content}`;
            return html`<img src="${imgurl}" alt="${this._actionResult?.name ?? this._actionResult?.path}" style="max-width: 100%;"/>`;
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
        if(this._selectedWorkspaceItem.isBinary){
            return this._renderBinaryContent();
        }else{
            return this._renderTextContent();
        }   
    }
    
    _renderBinaryContent(){
        
        if(this._selectedWorkspaceItem.type.startsWith("image")){
            let imgurl = `data:image/png;base64,${this._selectedWorkspaceItem.content}`;
            return html`<img src="${imgurl}" alt="${this._selectedWorkspaceItem.name}" style="max-width: 100%;"/>`;
        } else if(this._selectedWorkspaceItem.type === "application/pdf"){
            const dataUrl = `data:application/pdf;base64,${this._selectedWorkspaceItem.content}`;
            return html`<iframe .src=${dataUrl}></iframe>`;
        } else {
            return html`<qwc-workspace-binary 
                            base64Data="${this._selectedWorkspaceItem.content}" 
                            filename="${this._selectedWorkspaceItem.name}">
                        </qwc-workspace-binary>`;
        }
    }
    
    _renderTextContent(){
        return html`<qui-code-block id="code" class='codeBlock' @keydown="${this._onKeyDown}"
                        mode='${this._getMode(this._selectedWorkspaceItem.name)}'
                        theme='${themeState.theme.name}'
                        .content='${this._selectedWorkspaceItem.content}'
                        value='${this._selectedWorkspaceItem.content}'
                        showLineNumbers
                        editable>
                    </qui-code-block>`;
    }
    
    _renderConfirmDialog(){
        return html`
        <vaadin-confirm-dialog
            header="Unsaved changes"
            cancel-button-visible
            reject-button-visible
            reject-text="Discard"
            confirm-text="Save"
            .opened="${this._confirmDialogOpened}"
            @opened-changed="${this._confirmOpenedChanged}"
            @confirm="${() => {
                this._confirmSave();
            }}"
            @cancel="${() => {
                this._confirmCancel();
            }}"
            @reject="${() => {
                this._confirmDiscard();
            }}"
        >
          There are unsaved changes. Do you want to discard or save them?
        </vaadin-confirm-dialog>`;
    }
    
    _confirmOpenedChanged(e) {
        if(this._confirmDialogOpened !== e.detail.value){
            this._confirmDialogOpened = e.detail.value;
        }
    }
    
    _confirmSave(){
        this._saveSelectedWorkspaceItem();
        this._changeSelectedItem(this._changeToWorkspaceItem);
    }
    
    _confirmDiscard(){
        this._changeSelectedItem(this._changeToWorkspaceItem);
    }
    
    _confirmCancel(){
        this._changeToWorkspaceItem = { name: null, path: null, content: null, isDirty: false };
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
                this._selectedWorkspaceItem.content = jsonRpcResponse.result.result.content;
                this._selectedWorkspaceItem.type = e.detail.value.displayType;
                this._selectedWorkspaceItem.path = jsonRpcResponse.result.path;
                this._selectedWorkspaceItem.isDirty = true;
            }else if(e.detail.value.display !== "nothing"){
                this._actionResult = jsonRpcResponse.result.result;
                this._actionResult.name = this._actionResult.path;
                this._actionResult.path = jsonRpcResponse.result.path;
                this._actionResult.display = e.detail.value.display;
                this._actionResult.displayType = e.detail.value.displayType;
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
        let newWorkspaceItemValue = this._getChangedContent();
        if(newWorkspaceItemValue){
            this._saveContent(newWorkspaceItemValue, this._selectedWorkspaceItem.path);
        }
    }
    
    _copySelectedWorkspaceItem(){
        let content = this._selectedWorkspaceItem.content;
    
        let changedContent = this._getChangedContent();
        if(changedContent)content = changedContent;
        
        const path = this._selectedWorkspaceItem?.path;
        this._copyContent(content, path);
    }
    
    _saveActionResult(){
        if(this._actionResult && this._actionResult.content){
            this._saveContent(this._actionResult.content, this._actionResult.path, false);
        }
    }
    
    _copyActionResult(){
        if(this._actionResult && this._actionResult.content){
            this._copyContent(this._actionResult.content, this._actionResult.path);
        }
    }
    
    _saveContent(content, path, select=true){
        this.jsonRpc.saveWorkspaceItemContent({content:content, path:path}).then(jsonRpcResponse => { 
            if(jsonRpcResponse.result.success){
                notifier.showInfoMessage(jsonRpcResponse.result.path + " saved successfully");
                if(select) this._selectedWorkspaceItem = { ...this._selectedWorkspaceItem, content: content, isDirty: false };
            }else {
                notifier.showErrorMessage(jsonRpcResponse.result.path + " NOT saved. " + jsonRpcResponse.result.errorMessage);
            }
        });
        
    }
    
    _copyContent(content, path){
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
        this._selectWorkspaceItem(this._workspaceItems.get(event.detail.file));
    }
    
    _clearSelectedWorkspaceItem(){
        this._selectedWorkspaceItem = { name: null, path: null, content: null, isDirty: false };
        this._changeToWorkspaceItem = { name: null, path: null, content: null, isDirty: false };
    }
    
    _clearActionResult(){
        this._actionResult = null;
    }
    
    _selectWorkspaceItem(workspaceItem){
        if(workspaceItem.path !== this._selectedWorkspaceItem.path){
            if(this.shouldConfirmAwayNavigation()){
                this._changeToWorkspaceItem = workspaceItem;
                this._confirmDialogOpened = true;
            }else {
                this._changeSelectedItem(workspaceItem);
            }
        }
    }
    
    _changeSelectedItem(workspaceItem){
        this._clearSelectedWorkspaceItem();
        
        this.jsonRpc.getWorkspaceItemContent({path:workspaceItem.path}).then(jsonRpcResponse => {
            this._selectedWorkspaceItem = { ...this._selectedWorkspaceItem, 
                type: jsonRpcResponse.result.type,
                content: jsonRpcResponse.result.content,
                name:  workspaceItem.name,
                path:  workspaceItem.path,
                isBinary: jsonRpcResponse.result.isBinary
            };            
        });
        this._filterActions(workspaceItem.name);

        this._clearActionResult();
        this._showActionProgress = false;
    }
    
    shouldConfirmAwayNavigation(){
        if(this._selectedWorkspaceItem.isDirty) return true;
        let changedContent = this._getChangedContent();
        if(changedContent){
            return true;
        }
        return false;
    }
    
    _getChangedContent(){
        if(this._selectedWorkspaceItem.content){
            if(this._selectedWorkspaceItem.isDirty){
                return this._selectedWorkspaceItem.content;
            }else {
                let codeElement = this.shadowRoot.getElementById('code');
                if(codeElement){
                    let newWorkspaceItemValue = codeElement.getAttribute('value');
                    if(newWorkspaceItemValue!==this._selectedWorkspaceItem.content){
                        return newWorkspaceItemValue;
                    }
                }
            }
        }
        return null;
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