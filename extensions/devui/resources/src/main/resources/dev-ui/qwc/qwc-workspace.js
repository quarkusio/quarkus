import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import { RouterController } from 'router-controller';
import '@vaadin/button';
import '@vaadin/split-layout';
import '@vaadin/menu-bar';
import '@vaadin/tooltip';
import 'qui-themed-code-block';
import '@qomponent/qui-directory-tree';
import '@qomponent/qui-badge';
import '@vaadin/dialog';
import '@vaadin/confirm-dialog';
import '@vaadin/progress-bar';
import MarkdownIt from 'markdown-it';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { dialogHeaderRenderer, dialogFooterRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import { observeState } from 'lit-element-state';
import { notifier } from 'notifier';
import './qwc-workspace-binary.js';
import 'qui-ide-link';
import 'qui-assistant-warning';
import { assistantState } from 'assistant-state';

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
            flex-direction: column;
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
            position: absolute;
            right: 0px;
        }
    
        iframe {
            width: 100%;
            height: 100vh;
            border: none;
        }
    
        .mainMenuBar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-bottom: 1px solid var(--lumo-contrast-20pct);
        }
    
        .mainMenuBarButtons {
            display: flex; 
            align-items: center; 
        }

        .mainMenuBarTitle {
            font-size: large;
            color: var(--lumo-contrast-50pct);
            user-select: none;
            cursor: pointer;
            width: 100%;
            text-align: center;
        }

        .mainMenuBarActions {
            display: flex; 
            align-items: center; 
            gap: 0.5rem;
            justify-content: end;
            padding-right: 10px;
        }

        .assistant {
            position: absolute;
            top: 0;
            right: 0;
            padding-top: 8px;
            padding-right: 16px;
            z-index:9;
        }
    
        .actionResult {
            display: flex;
            flex-direction: column;
            gap: 5px;
        }
    
        vaadin-progress-bar {
            width: 20%;
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
        _confirmDialogOpened: {state: true},
        _isMaximized: {state: true}
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
        this._isMaximized = false;
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
                            ${this._renderMainMenuBar()}
                            <vaadin-split-layout>
                                <master-content style="width: ${this._isMaximized ? '0%' : '25%'};">${this._renderWorkspaceTree()}</master-content>
                                <detail-content style="width: ${this._isMaximized ? '100%' : '75%'};">${this._renderSelectedSource()}</detail-content>
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
                                <div class="mainPart">
                                    ${this._renderMainContent()}
                                </div>
                            </master-content>
                            ${this._renderResultSplitView()}
                        </vaadin-split-layout>
                    `;
        }
    }
    
    _renderMainMenuBar(){
        const isHidden = !this._selectedWorkspaceItem.name;
        
        return html`
            <div class="mainMenuBar" style="${isHidden ? 'visibility: hidden;' : ''}">
                <div class="mainMenuBarButtons">
                    <vaadin-button title="Save" theme="icon tertiary" aria-label="Save" @click="${this._saveSelectedWorkspaceItem}">
                        <vaadin-icon icon="font-awesome-solid:floppy-disk"></vaadin-icon>
                    </vaadin-button>
                    <vaadin-button title="Copy" theme="icon tertiary" aria-label="Copy" @click="${this._copySelectedWorkspaceItem}">
                        <vaadin-icon icon="font-awesome-solid:copy"></vaadin-icon>
                    </vaadin-button>
                </div>

                <div class="mainMenuBarTitle" @dblclick="${this._toggleSplit}">
                    ${this._selectedWorkspaceItem?.name?.split('/').pop()}
                </div>

                <div class="mainMenuBarActions">
                    ${this._renderActions()}
                    <qui-ide-link title="Open in IDE"
                        style="cursor: pointer;"
                        fileName="${this._selectedWorkspaceItem?.path}"
                        lineNumber="0"
                        noCheck>
                        <vaadin-icon icon="font-awesome-solid:up-right-from-square"></vaadin-icon>
                    </qui-ide-link>
                </div>
            </div>
        `;
        
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
            return html`<div class="actionResult">${this._actionResult.content}</div>${this._renderAssistantWarning()}`;
        }else if(this._actionResult && this._actionResult.content && this._actionResult.displayType === "code"){
            // TODO: We can not assume the mode is the same as the input
            // Maybe return name|content ?
            return html`<div class="actionResult">
                            ${this._renderAssistantWarning()}    
                            <qui-themed-code-block id="code" class='codeBlock'
                                mode='${this._getMode(this._actionResult?.name ?? this._actionResult?.path)}' 
                                .content='${this._actionResult.content}'
                                showLineNumbers>
                            </qui-themed-code-block>
                        </div>`;
        }else if(this._actionResult && this._actionResult.content && this._actionResult.displayType === "markdown"){
            const htmlContent = this.md.render(this._actionResult.content);
            return html`<div class="actionResult">
                            ${this._renderAssistantWarning()}
                            ${unsafeHTML(htmlContent)}
                        </div>`; 
        }else if(this._actionResult && this._actionResult.content && this._actionResult.displayType === "html"){
            return html`<div class="actionResult">
                            ${this._renderAssistantWarning()}
                            ${unsafeHTML(this._actionResult.content)}
                        </div>`; 
        }else if(this._actionResult && this._actionResult.content && this._actionResult.displayType === "image"){
            let imgurl = `data:image/png;base64,${this._actionResult.content}`;
            return html`<div class="actionResult">
                            ${this._renderAssistantWarning()}
                            <img src="${imgurl}" alt="${this._actionResult?.name ?? this._actionResult?.path}" style="max-width: 100%;"/>
                        </div>`;
        }
    }
    
    _renderActions(){
        if(this._filteredActions){
            if(this._showActionProgress){
                return html`<vaadin-progress-bar style="width:400px;" indeterminate></vaadin-progress-bar>`;
            }else{
                return html`<div class="actions">
                            <vaadin-menu-bar .items="${this._filteredActions}" theme="dropdown-indicators tertiary" @item-selected="${(e) => this._actionSelected(e)}"></vaadin-menu-bar>
                        </div>`;
            }
        }
    }
    
    _renderMainContent(){
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
        return html`<qui-themed-code-block id="code" class='codeBlock' @keydown="${this._onKeyDown}"
                        mode='${this._getMode(this._selectedWorkspaceItem.name)}'
                        .content='${this._selectedWorkspaceItem.content}'
                        value='${this._selectedWorkspaceItem.content}'
                        showLineNumbers
                        editable>
                    </qui-themed-code-block>
                    ${this._renderAssistantWarningInline()}`;
    }

    _renderAssistantWarningInline(){
        if(this._selectedWorkspaceItem.isAssistant){
            return html`<qui-assistant-warning class="assistant"></qui-assistant-warning>`;
        }
    }

    _renderAssistantWarning(){
        if(this._actionResult.isAssistant){
             return html`<qui-assistant-warning></qui-assistant-warning>`;
        }   
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
    
    _toggleSplit() {
        this._isMaximized = !this._isMaximized;
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
            
            if (!('content' in jsonRpcResponse.result.result) || !('name' in jsonRpcResponse.result.result)) {
                const firstEntry = Object.entries(jsonRpcResponse.result.result).find(
                    ([key]) => key !== 'path' && key !== 'name'
                );
                if (firstEntry) {
                    const [key, value] = firstEntry;
                    if (!('content' in jsonRpcResponse.result.result)) {
                        jsonRpcResponse.result.result.content = value;
                    }
                    if (!('name' in jsonRpcResponse.result.result)) {
                        jsonRpcResponse.result.result.name = key;
                    }
                }
            }
            
            if(e.detail.value.display === "notification"){
                notifier.showInfoMessage(jsonRpcResponse.result.result);
            }else if(e.detail.value.display === "replace"){
                this._selectedWorkspaceItem.content = jsonRpcResponse.result.result.content;
                this._selectedWorkspaceItem.type = e.detail.value.displayType;
                this._selectedWorkspaceItem.path = jsonRpcResponse.result.path;
                this._selectedWorkspaceItem.isDirty = true;
                this._selectedWorkspaceItem.isAssistant = jsonRpcResponse.result?.isAssistant ?? false;
            }else if(e.detail.value.display !== "nothing"){
                this._actionResult = jsonRpcResponse.result.result;
                if(jsonRpcResponse.result.result.name){
                    this._actionResult.name = jsonRpcResponse.result.result.name;
                }else{
                    this._actionResult.name = this._actionResult.path;
                }
                this._actionResult.path = jsonRpcResponse.result.path;
                this._actionResult.display = e.detail.value.display;
                this._actionResult.isAssistant = jsonRpcResponse.result?.isAssistant ?? false;
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
                                displayType: item.displayType,
                                isAssistanceAction: item.isAssistanceAction
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
                    if(regex.test(name)){
                        if(child.isAssistanceAction){
                            return assistantState.current.isConfigured;
                        }
                        return true;
                    }
                    return false;
                }else if(child.isAssistanceAction){
                    return assistantState.current.isConfigured;
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