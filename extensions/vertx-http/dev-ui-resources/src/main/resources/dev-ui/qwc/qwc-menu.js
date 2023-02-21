import { LitElement, html, css} from 'lit';
import { devuiState } from 'devui-state';
import { observeState } from 'lit-element-state';
import { RouterController } from 'router-controller';
import '@vaadin/icon';

/**
 * This component represent the Dev UI left menu
 * It creates the menuItems during build and dynamically add the routes and import the relevant components
 */
export class QwcMenu extends observeState(LitElement) {
    
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
                color: var(--lumo-contrast);
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
    };
    
    constructor() {
        super();
        // TODO, Use state for location
        window.addEventListener('vaadin-router-location-changed', (event) => {
            this._updateSelection(event);
        });
    }
    
    connectedCallback() {
        super.connectedCallback();
        this._selectedPage = "qwc-extensions"; // default
        this._selectedPageLabel = "Extensions"; // default
        this._restoreState();
    }
    
    _restoreState(){
        const storedState = localStorage.getItem("qwc-menu-state");
        if(storedState && storedState === "small"){
            this._smaller();
        }else{
            this._larger();
        }
    }
    
    _updateSelection(event){
        var pageDetails = RouterController.parseLocationChangedEvent(event);
        this._selectedPage = pageDetails.component;
        this._selectedPageLabel = pageDetails.title;
    }

    render() {
        return html`
            <div class="left">
                <div class="menu" style="width: ${this._width}px;" @dblclick=${this._doubleClicked}>
                    ${devuiState.menu.map((menuItem, index) =>
                        html`${this._renderItem(menuItem, index)}`
                    )}
                    ${this._renderIcon("chevron-left", "smaller")}
                    ${this._renderIcon("chevron-right", "larger")}
                </div>

                ${this._renderVersion()}
            </div>`;
    }

    _renderVersion(){
        if(this._show){
            return html`<div class="quarkusVersion">
                            <span @click="${this._quarkus}">Quarkus ${devuiState.applicationInfo.quarkusVersion}</span>
                        <div>`;
        }
    }

    _renderItem(page, index){
        
        var pagename = page.componentName;
        var defaultSelection = false;
        if(index===0)defaultSelection = true;
        import(page.componentRef);
        RouterController.addMenuRoute(page, defaultSelection);
        
        let displayName = "";
        if(this._show){
            displayName = page.title;
        }
        let pageRef = RouterController.pageRefWithBase(page.componentName);
        
        const selected = this._selectedPage == page.componentName;
        let classnames = "item";
        if(selected){
            classnames = "item selected";
        }

        return html`
        <a class="${classnames}" href="${pageRef}">
            <vaadin-icon icon="${page.icon}"></vaadin-icon>
            <span class="item-text" data-page="${page.componentName}">${displayName}</span>
        </a>
        `;        
    }

    _renderIcon(icon, action){
        if((action == "smaller" && this._show) || (action == "larger" && !this._show)){
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
        localStorage.setItem('qwc-menu-state', "small");
    }

    _larger() {
        this._show = true;
        this._width = 250;
        localStorage.setItem('qwc-menu-state', "large");
    }
    
    _quarkus(e) {
        window.open("https://quarkus.io", '_blank').focus();
    }
}

customElements.define('qwc-menu', QwcMenu);