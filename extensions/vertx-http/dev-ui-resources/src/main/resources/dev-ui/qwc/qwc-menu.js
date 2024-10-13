import { LitElement, html, css} from 'lit';
import { devuiState } from 'devui-state';
import { observeState } from 'lit-element-state';
import { RouterController } from 'router-controller';
import { StorageController } from 'storage-controller';
import '@vaadin/icon';
import '@vaadin/context-menu';

/**
 * This component represent the Dev UI left menu
 * It creates the menuItems during build and dynamically add the routes and import the relevant components
 */
export class QwcMenu extends observeState(LitElement) {
    routerController = new RouterController(this);
    storageControl = new StorageController(this);
    
    static styles = css`
            .left {
                height: 100%;
                display: flex;
                flex-direction: column;
                justify-content: space-between;
            }

            .menu {
                height: 100%;
                display: flex;
                flex-direction: column;
            }
            
            .menuSizeControl {
                align-self: flex-end;
                cursor: pointer;
                color: var(--lumo-contrast-10pct);
                height: 60px;
                width: 30px;
                padding-top:30px;
            }
            
            .menuSizeControl:hover {
                color: var(--lumo-primary-color-50pct);
            }

            @media screen and (max-width: 1280px) {
                .menuSizeControl, .quarkusVersion, .item-text {
                    display: none;
                }
                .menu, .left {
                    width: 35px!important;
                }
                vaadin-icon {
                    width: var(--lumo-icon-size-s);
                    height: var(--lumo-icon-size-s);
                }
            }

            .item {
                display: flex;
                flex-direction: row;
                align-items:center;
                padding-top: 10px;
                padding-bottom: 10px;
                padding-left: 5px;
                gap: 10px;
                cursor: pointer;
                border-left: 5px solid transparent;
                color: var(--lumo-contrast-90pct);
                height:30px;
                text-decoration: none;
            }
            
            .item:hover{
                border-left: 5px solid var(--lumo-primary-color);
                background-color: var(--lumo-primary-color-10pct);
            }

            .selected {
                border-left: 5px solid var(--lumo-primary-color);
                cursor: default;
                background-color: var(--lumo-primary-color-10pct);
            }

            .hidden {
                display:none;
            }

            .quarkusVersion {
                padding-bottom: 10px;
                padding-left: 15px;
                width: 100%;
            }
    
            .quarkusVersion span {
                cursor: pointer;
                font-size: small;
                color: var(--lumo-contrast-50pct);
            }
    
            .quarkusVersion span:hover {
                color: var(--lumo-primary-color-50pct);
            }
        `;

    static properties = {
        _show: {state: true},
        _selectedPage: {attribute: false},
        _selectedPageLabel: {attribute: false},
        _width: {state: true},
        _customMenuNamespaces: {state: true},
        _dynamicMenuNamespaces: {state: true},
    };
    
    constructor() {
        super();
        // TODO, Use state for location
        window.addEventListener('vaadin-router-location-changed', (event) => {
            this._updateSelection(event);
        });
        this._customMenuNamespaces = [];
        this._dynamicMenuNamespaces = null;
    }
    
    connectedCallback() {
        super.connectedCallback();
        this._selectedPage = "devui-extensions"; // default
        this._selectedPageLabel = "Extensions"; // default
        this._dynamicMenuNamespaces = this._restoreDynamicMenuItems();
        this._restoreState();
    }
    
    _restoreState(){
        const storedState = this.storageControl.get("state");
        if(storedState && storedState === "small"){
            this._smaller();
        }else{
            this._larger();
        }
    }
    
    _updateSelection(event){
        let currentPage = this.routerController.getCurrentPage();
        this._selectedPageLabel = currentPage.title;
        this._selectedPage = currentPage.namespace;
        this._selectedPageIsMax = !currentPage.includeInMenu; // TODO: introduce new property called isMaxView ?
    }

    render() {
        this._customMenuNamespaces = [];
        let classnames = this._getClassNamesForMenu();
        return html`
            <div class="${classnames}">
                <div class="menu" style="width: ${this._width}px;" @dblclick=${this._doubleClicked} @dragover="${this._handleDragOver}" @drop="${this._handleDrop}">
                    ${devuiState.menu.map((menuItem, index) =>
                        html`${this._renderItem(menuItem, index)}`
                    )}
                    ${this._dynamicMenuNamespaces.map((page) =>
                        html`${this._renderCustomItem(page, -1)}`
                    )}
                    ${this._renderIcon("chevron-left", "smaller")}
                    ${this._renderIcon("chevron-right", "larger")}
                </div>

                ${this._renderVersion()}
            </div>`;
    }

    _handleDragOver(event) {
        event.preventDefault();
    }
    
    _handleDrop(event) {
        event.preventDefault();
    
        const data = event.dataTransfer.getData('application/json');
        const customMenu = JSON.parse(data);
    
        let storedMenu = this._restoreDynamicMenuItems();
        
        const index = storedMenu.findIndex(obj => obj.id === customMenu.id);
        if (index === -1) {
            storedMenu.push(customMenu);
        }
        
        this._storeDynamicMenuItems(storedMenu);
        this._dynamicMenuNamespaces = this._restoreDynamicMenuItems();
    }

    _restoreDynamicMenuItems(){
        let menu = this.storageControl.get('customPageLinks');
        if(menu){
            return JSON.parse(menu);
        }else{
            return [];
        }
    }
    
    _storeDynamicMenuItems(menu){
        this.storageControl.set('customPageLinks', JSON.stringify(menu));
    }

    _renderVersion(){
        if(this._show){
            return html`<div class="quarkusVersion">
                            <span @click="${this._quarkus}">Quarkus ${devuiState.applicationInfo.quarkusVersion}</span>
                        <div>`;
        }
    }

    _renderCustomItem(page, index){
        if(page){
            const index = devuiState.cards.active.findIndex(obj => obj.namespace === page.namespace); // Only show if that extension is added
            if (index !== -1) {
            
                let extensionName = "";
                if(page.metadata && page.metadata.extensionName){
                    extensionName = page.metadata.extensionName;
                }
            
                let items = [{ text: 'Remove', action: 'remove', id: page.id , namespace: page.namespace}];
                return html`<vaadin-context-menu .items=${items} @item-selected=${this._handleContextMenu} title="${extensionName}">
                            ${this._renderItem(page, index)}
                        </vaadin-context-menu>`;
            }
        }
    }
    
    _handleContextMenu(event){
        const selectedItem = event.detail.value;
        if (selectedItem && selectedItem.action === 'remove') {
            let storedMenu = this._restoreDynamicMenuItems();
            const index = storedMenu.findIndex(obj => obj.id === selectedItem.id);
            if (index !== -1) {
                storedMenu.splice(index, 1);
            }
            this._storeDynamicMenuItems(storedMenu);
            this._dynamicMenuNamespaces = this._restoreDynamicMenuItems();  
        }
    }
    
    _renderItem(page, index){
        
        var defaultSelection = false;
        if(index===0)defaultSelection = true;
        if(page.componentRef){
            import(page.componentRef);
            this.routerController.addRouteForMenu(page, defaultSelection);
        }
        
        // Each namespace has one place on the menu
        if(!this._customMenuNamespaces.includes(page.namespace)){
            this._customMenuNamespaces.push(page.namespace);
            let displayName = "";
            if(this._show){
                if(page.namespaceLabel){
                    displayName = page.namespaceLabel;
                }else{
                    displayName = page.title;
                }
            }
            
            let pageRef = this.routerController.getPageUrlFor(page);
            
            let classnames = this._getClassNamesForMenuItem(page, index);
            return html`
                <a class="${classnames}" href="${pageRef}">
                    <vaadin-icon icon="${page.icon}"></vaadin-icon>
                    <span class="item-text" data-page="${page.componentName}">${displayName}</span>
                </a>
                `;
        }
    }

    _getClassNamesForMenu(){
        if(this._selectedPageIsMax){
            return "hidden";
        }
        return "left";
    }

    _getClassNamesForMenuItem(page, index){
        if(!page.includeInMenu ){
            return "hidden";
        }
        
        const selected = this._selectedPage == page.namespace;
        if(selected){
            return "item selected";
        }
        
        // Else check for default
        let pages = devuiState.menu;
        let hasMenuItem = false;
        for (let i = 0; i < pages.length; i++) {
            if(this._selectedPage === pages[i].namespace){
                hasMenuItem = true;
            }
        }

        if(!hasMenuItem && index === 0){
            return "item selected";
        }
        
        return "item";
    }

    _renderIcon(icon, action){
        if(action == "smaller" && this._show){
            return html`
                <vaadin-icon class="menuSizeControl" icon="font-awesome-solid:${icon}" @click="${this._changeMenuSize}" data-action="${action}" style="position: absolute;top: 45%;"></vaadin-icon>
            `;
        }else if(action == "larger" && !this._show){
            return html`
                <vaadin-icon class="menuSizeControl" icon="font-awesome-solid:${icon}" @click="${this._changeMenuSize}" data-action="${action}"></vaadin-icon>
            `;
        }
    }

    _doubleClicked(e) {    
        if(e.target.tagName.toLowerCase() === "div"){
            if(this._show){
                this._smaller();
            }else {
                this._larger();
            }
        } 
    }

    _changeMenuSize(e){
        if(e.target.dataset.action === "smaller"){
            this._smaller();
        }else{
            this._larger();
        }
        this.requestUpdate();
    }

    _smaller() {
        this._show = false;
        this._width = 50;
        this.storageControl.set('state', "small");
    }

    _larger() {
        this._show = true;
        this._width = 250;
        this.storageControl.set('state', "large");
    }
    
    _quarkus(e) {
        window.open("https://quarkus.io", '_blank').focus();
    }
}

customElements.define('qwc-menu', QwcMenu);