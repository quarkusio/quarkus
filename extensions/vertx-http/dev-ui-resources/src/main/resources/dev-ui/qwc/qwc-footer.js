import { LitElement, html, css} from 'lit';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { LogController } from 'log-controller';
import { StorageController } from 'storage-controller';
import { devuiState } from 'devui-state';
import { observeState } from 'lit-element-state';
import '@vaadin/tabsheet';
import '@vaadin/tabs';
import '@vaadin/icon';
import '@vaadin/menu-bar';
import 'qwc/qwc-ws-status.js';
        
/**
 * This component shows the Bottom Drawer
 * 
 */
export class QwcFooter extends observeState(LitElement) {
        
    storageControl = new StorageController(this);
    
    static styles = css`
    
        .openIcon {
            cursor: pointer;
            font-size: var(--lumo-font-size-s);
        }
        
        .openIcon:hover {
            color: var(--quarkus-blue);
        }
        
        #footer {
            background: var(--lumo-contrast-5pct);
            overflow: hidden;
            margin-right: 5px;
            margin-left: 5px;
            border-radius: 15px 15px 0px 0px;
            display: flex;
            flex-direction: column;
        }
        .footerOpen {
            
        }
        .footerClose {
            max-height: 38px;
            overflow: hidden;
        }
        
        .dragOpen {
            overflow: hidden;
            height: 4px;
            cursor: row-resize;
            background: var(--lumo-contrast-10pct);
        }
    
        .resizeIcon {
            display: none;
        }
    
        @media screen and (max-width: 1600px) {
            .dragOpen {
                height: 5px;
                background: var(--lumo-contrast-10pct);
            }
        }
    
        @media screen and (max-width: 1280px) {
            .dragOpen {
                height: 6px;
                background: var(--lumo-contrast-20pct);
            }
            #footer {
                margin-right: 0px;
                margin-left: 0px;
                border-radius: 0px 0px 0px 0px;
            }
            .resizeIcon {
                display: inline;
            }
        }
    
        @media screen and (max-width: 1152px) {
            .dragOpen {
                height: 7px;
                background: var(--lumo-contrast-20pct);
            }
        }
    
        @media screen and (max-width: 1024px) {
            .dragOpen {
                height: 7px;
                background: var(--lumo-contrast-30pct);
            }
        }
    
        @media screen and (max-width: 900px) {
            .dragOpen {
                height: 8px;
                background: var(--lumo-contrast-30pct);
            }
        } 
        
        vaadin-menu-bar-button {
            background-color: transparent;
        }
        
        vaadin-menu-bar-button:hover {
            background-color: transparent;
        }
    
        .dragOpen:hover {
            background: var(--quarkus-blue);
        }
        
        .dragClose {
            display:none;
        }
        
        vaadin-tabsheet {
            overflow: hidden;
        }
        .tabsheetOpen {
            height: 100%;
        }
        .tabsheetClose {
            max-height: 38px;
            justify-content: flex-start;
        }
        
        vaadin-tabs {
            max-width: 100%; 
            overflow: hidden;
        }
        .tabsOpen {
            height: 100%;
        }
    
        .tabsClose {
            max-height: 0px;
            visibility: collapse;
        }
        
        vaadin-tab {
            overflow: hidden;
        }
        .tabOpen {
            
        }
        .tabClose {
            max-height: 0px;
            visibility: hidden;
        }
        
        .controlsOpen {
        }
        
        .controlsClose {
            display:none;
        }
    
        .tabContentOpen {
            font-size: var(--lumo-font-size-s);
            overflow: auto;
        }
    
        .tabContentClose {
            font-size: var(--lumo-font-size-s);
            overflow: hidden;
            max-height: 0px;
            visibility: hidden;
        }
        
    `;

    static properties = {
        _isOpen: {state: true},
        _footerClass: {state: false},
        _tabsheetClass: {state: false},
        _tabsClass: {state: false},
        _tabClass: {state: false},
        _tabContentClass: {state: false},
        _dragClass: {state: false},
        _controlsClass: {state: false},
        _arrow: {state: false},
        _controlButtons: {state: true},
        _selectedTab: {state: false},
        _height: {state: false},
        _originalHeight: {state: false},
        _originalMouseY: {state: false},
    };

    constructor() {
        super();
        LogController.addListener(this);
    }

    loaded(){
        this._initControlButtons();
    }

    connectedCallback() {
        super.connectedCallback();
        this._controlButtons = [];
        this._originalMouseY = 0;
        
        this._restoreState();
        this._restoreHeight();
        this._restoreSelectedTab();
    }

    _restoreHeight(){
        const storedHeight = this.storageControl.get("height");
        if(storedHeight){
            this._height = storedHeight;
        }else {
            this._height = 250; // Default initial height
        }
        this._originalHeight = this._height;
    }

    _restoreState(){
        const storedState = this.storageControl.get("state");
        if(storedState && storedState === "open"){
            this._open();
        }else {
            this._close();
        }
    }

    _restoreSelectedTab(){
        const storedTab = this.storageControl.get("selected-tab");
        if(storedTab){
            this._tabSelected(storedTab);
        }else {
            this._tabSelected(0);
        }
    }

    render() {
        
        var selectedComponentName = devuiState.footer[this._selectedTab];
        if(!selectedComponentName){ // Might have been removed
            this._tabSelected(0);
        }
        
            return html`<div id="footer" class="${this._footerClass}" style="height: ${this._height}px;" @dblclick=${this._doubleClicked}>
                        <div class="${this._dragClass}" @mousedown=${this._mousedown}></div>
                        <vaadin-tabsheet theme="minimal" class="${this._tabsheetClass}">
        
                            ${this._renderTabBodies()}
        
                            <qwc-ws-status slot="prefix"></qwc-ws-status>
                            <vaadin-icon slot="prefix" class="openIcon" icon="font-awesome-solid:chevron-${this._arrow}" @click=${this._openCloseClicked}></vaadin-icon>
                            
                            <vaadin-tabs slot="tabs" class="${this._tabsClass}" theme="small" selected=${this._selectedTab}>
                                ${this._renderTabHeaders()}
                            </vaadin-tabs>
        
                            <div class="${this._controlsClass}" slot="suffix">
                                ${this._renderControls()}
                            </div>
        
                            ${this._renderResizeIcon()}
                            
                        </vaadin-tabsheet>
                    </div>`;
    }

    _renderTabHeaders(){
        return html`${devuiState.footer.map((footerTab, index) =>
                this._renderTabHeader(footerTab, index)
            )}`;
    }
    
    _renderTabHeader(footerTab, index){
        import(footerTab.componentRef);
        return html`<vaadin-tab id="tab-${index}" class="${this._tabClass}" @click=${() => this._tabSelected(index)}>${footerTab.title}</vaadin-tab>`;
    }
    
    _renderControls(){
        return html`<vaadin-menu-bar
                            theme="small"
                            .items="${this._controlButtons}" 
                            @item-selected="${this._controlButtonClicked}">
                        </vaadin-menu-bar>`;
    }
    
    _renderTabBodies(){
        
        return html`${devuiState.footer.map((footerTab, index) =>
                html`<div class="${this._tabContentClass}" tab="tab-${index}">
                    ${this._renderTabBody(footerTab)}
                </div>`
            )}`;
    }

    _renderTabBody(footerTab){
        if(footerTab.componentName === "qwc-footer-log"){ // Reusable footer log.
            let jsonRpcMethodName = footerTab.metadata.jsonRpcMethodName;
            let dynamicFooter = `<${footerTab.componentName} title="${footerTab.title}" namespace="${footerTab.namespace}" jsonRpcMethodName="${jsonRpcMethodName}"></${footerTab.componentName}>`;
            return html`${unsafeHTML(dynamicFooter)}`;
        }else{
            let dynamicFooter = `<${footerTab.componentName} title="${footerTab.title}" namespace="${footerTab.namespace}"></${footerTab.componentName}>`;
            return html`${unsafeHTML(dynamicFooter)}`;
        }
    }

    _tabSelected(index){
        this._selectedTab = index;
        var selectedComponentName = devuiState.footer[this._selectedTab];
        if(selectedComponentName){
            this._controlButtons = LogController.getItemsForTab(devuiState.footer[this._selectedTab].title);
        }else{
            this._controlButtons = LogController.getItemsForTab(devuiState.footer[0].title);
            this._selectedTab = 0;
        }
        this.storageControl.set('selected-tab', this._selectedTab);
    }

    _renderResizeIcon(){
        return html`<vaadin-icon slot="suffix" class="resizeIcon" icon="font-awesome-solid:up-down" @mousedown=${this._mousedown}></vaadin-icon>`;
    }

    _mousedown(e){
        this._originalHeight = this._height;
        this._originalMouseY = e.y;
        document.addEventListener('mousemove', this._mousemove, true);
        document.addEventListener('mouseup', this._mouseup, true);
    }

    _mousemove = (e) => {
        const height = this._originalHeight - (e.y - this._originalMouseY);
        this._height = height;
        
        // Snap close
        if(this._height<=70){
            this._height = this._originalHeight;
            this._close();
            this._mouseup();
        }
    }

    _mouseup = (e) => {
        document.removeEventListener('mousemove', this._mousemove, true);
        document.removeEventListener('mouseup', this._mouseup, true);
        
        if(this._height){
            this.storageControl.set('height', this._height);
        }
    }

    _doubleClicked(e) {
        if(e.target.tagName.toLowerCase() === "vaadin-tabs" 
                || e.target.tagName.toLowerCase() === "vaadin-tabsheet"){
            
                this._openCloseClicked(e);
        }
    }
    
    _openCloseClicked(e){
        if(this._isOpen){
            this._close();
        }else {
            this._open();
        }
    }

    _initControlButtons(){
        if (this._controlButtons.length === 0) {
            if(this._selectedTab){
                this._tabSelected(this._selectedTab);
            }else{
                this._tabSelected(0);
            }
        }
    }
    
    _open(){
        this._arrow = "down";
        this._footerClass = "footerOpen";
        this._tabsheetClass = "tabsheetOpen";
        this._tabsClass = "tabsOpen";
        this._tabClass = "tabOpen";
        this._tabContentClass = "tabContentOpen";
        this._dragClass = "dragOpen";
        this._controlsClass = "controlsOpen";
        this._isOpen=true;
        this.storageControl.set('state', "open");
    }
    
    _close(){
        this._arrow = "up";
        this._footerClass = "footerClose";
        this._tabsheetClass = "tabsheetClose";
        this._tabsClass = "tabsClose";
        this._tabClass = "tabClose";
        this._tabContentClass = "tabContentClose";
        this._dragClass = "dragClose";
        this._controlsClass = "controlsClose";
        this._isOpen=false;
        this.storageControl.set('state', "close");
    }
    
    _controlButtonClicked(e){
        LogController.fireCallback(e);
    }
}
customElements.define('qwc-footer', QwcFooter);