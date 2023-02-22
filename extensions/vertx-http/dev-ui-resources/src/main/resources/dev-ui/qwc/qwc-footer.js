import { LitElement, html, css} from 'lit';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { LogController } from 'log-controller';
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
        
    static styles = css`
    
        vaadin-menu-bar {
            --lumo-size-m: 10px;
            --lumo-space-xs: 0.5rem;
            --_lumo-button-background-color: transparent;
        }
        
        .openIcon {
            cursor: pointer;
            font-size: small;
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
            height: 3px;
            cursor: row-resize;
            background: var(--lumo-contrast-10pct);
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

    connectedCallback() {
        super.connectedCallback();
        this._controlButtons = [];
        this._originalMouseY = 0;
        
        this._restoreState();
        this._restoreHeight();
        this._restoreSelectedTab();
    }

    _restoreHeight(){
        const storedHeight = localStorage.getItem("qwc-footer-height");
        if(storedHeight){
            this._height = storedHeight;
        }else {
            this._height = 250; // Default initial height
        }
        this._originalHeight = this._height;
    }

    _restoreState(){
        const storedState = localStorage.getItem("qwc-footer-state");
        if(storedState && storedState === "open"){
            this._open();
        }else {
            this._close();
        }
    }

    _restoreSelectedTab(){
        const storedTab = localStorage.getItem("qwc-footer-selected-tab");
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
                            <vaadin-icon slot="prefix" class="openIcon" icon="font-awesome-solid:chevron-${this._arrow}" @click=${this._doubleClicked}></vaadin-icon>
                            
                            <vaadin-tabs slot="tabs" class="${this._tabsClass}" theme="small" selected=${this._selectedTab}>
                                ${this._renderTabHeaders()}
                            </vaadin-tabs>
        
                            <div class="${this._controlsClass}" slot="suffix">
                                ${this._renderControls()}
                            </div>
        
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
        return html`${unsafeHTML('<' + footerTab.componentName + '></' + footerTab.componentName + '>')}`;
    }

    _tabSelected(index){
        this._selectedTab = index;
        var selectedComponentName = devuiState.footer[this._selectedTab];
        if(selectedComponentName){
            this._controlButtons = LogController.getItemsForTab(devuiState.footer[this._selectedTab].componentName);
        }else{
            this._controlButtons = LogController.getItemsForTab(devuiState.footer[0].componentName);
            this._selectedTab = 0;
        }
        localStorage.setItem('qwc-footer-selected-tab', this._selectedTab);
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
        
        if(this._height<=70){
            this._height = this._originalHeight;
            this._doubleClicked(e);
            this._mouseup();
        }
    }

    _mouseup = (e) => {
        document.removeEventListener('mousemove', this._mousemove, true);
        document.removeEventListener('mouseup', this._mouseup, true);
        
        if(this._height){
            localStorage.setItem('qwc-footer-height', this._height);
        }
    }

    _doubleClicked(e) {
        if(e.target.tagName.toLowerCase() === "vaadin-tabs" 
                || e.target.tagName.toLowerCase() === "vaadin-tabsheet"
                || e.target.tagName.toLowerCase() === "vaadin-icon"){
            if(this._isOpen){
                this._close();
            }else {
                this._open();
            }
        }
        
        // Initial load of control buttons
        if (this._controlButtons.length === 0) {
            this._initControlButtons();
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
        localStorage.setItem('qwc-footer-state', "open");
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
        localStorage.setItem('qwc-footer-state', "close");
    }
    
    _controlButtonClicked(e){
        LogController.fireCallback(e);
    }
}
customElements.define('qwc-footer', QwcFooter);