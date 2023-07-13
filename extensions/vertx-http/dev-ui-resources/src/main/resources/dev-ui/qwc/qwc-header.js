import { LitElement, html, css } from 'lit';
import { RouterController } from 'router-controller';
import { StorageController } from 'storage-controller';
import { notifier } from 'notifier';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import { devuiState } from 'devui-state';
import '@vaadin/menu-bar';
import '@vaadin/tabs';
import '@vaadin/button';
import 'qwc/qwc-extension-link.js';

/**
 * This component represent the Dev UI Header
 */
export class QwcHeader extends observeState(LitElement) {
    
    storageControl = new StorageController(this);
    routerController = new RouterController(this);
    
    static styles = css`
        
        .top-bar {
            height: 70px;
            display: flex;
            align-items: center;
            flex-direction: row;
            justify-content: space-between;
        }
        .right-bar {
            display: flex;
            justify-content: space-around;
            align-items: center;
        }
        
        .logo-title {
            display: flex;
            align-items: center;
            flex-direction: row;
        }
        .top-bar svg {
            height: 45px;
            padding: 8px;
        }

        .logo-right-actions {
            display: flex;
            align-items:center;
            padding-right: 10px;
        }
        
        .logo-reload-click {
            cursor: pointer;
            display: flex;
            align-items:center;
        }

        .logo-reload-click:hover {
            filter: brightness(90%);
        }

        .title {
            display: flex;
            align-items:center;
            font-size: var(--lumo-font-size-xl);
            padding-left: 100px;
            color: var(--lumo-contrast-90pct);
        }
        
        .logo-text {
            padding-top: 10px;
            font-size: xx-large;
        }
    
        .app-info {
            font-size: var(--lumo-font-size-s);
            color: var(--lumo-contrast-50pct);
            display: flex;
            align-items: center;
            gap: 10px;
        }
    
        .themeDropdown {
            padding-right: 15px;
            padding-left: 15px;
            width: 60px;
        }
        `;

    static properties = {
        _title: {state: true},
        _rightSideNav: {state: true},
        _selectedTheme: {state: true},
        _themeOptions: {state: true},
        _desktopTheme: {state: true}
    };

    constructor() {
        super();
        this._title = "Extensions";
        this._rightSideNav = "";
        
        this._createThemeItems();
        this._restoreThemePreference();
        this._createThemeOptions();
        
        window.addEventListener('vaadin-router-location-changed', (event) => {
            this._updateHeader(event);
        });
    }

    connectedCallback() {
        super.connectedCallback();
        // Get desktop theme setting
        this._desktopTheme = "dark";
        if(window.matchMedia){
             if(window.matchMedia('(prefers-color-scheme: light)').matches){
                this._desktopTheme = "light";
             }
            
             // Change theme setting when OS theme change
             window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', e => {
                 if(e.matches){
                    this._desktopTheme = "light";
                 }else{
                    this._desktopTheme = "dark";
                 }
                 this._changeToSelectedTheme();
            });
        }
        
        this._changeToSelectedTheme();
    }

    _restoreThemePreference() {
        const storedValue = this.storageControl.get("theme-preference");
        if(storedValue){
            this._selectedTheme = storedValue;
        }else {
            this._selectedTheme = "desktop";
        }
    }

    render() {
        return html`
        <div class="top-bar">
            <div class="logo-title">
                <div class="logo-reload-click">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024"><defs><style>.cls-1{fill:${themeState.theme.quarkusBlue};}.cls-2{fill:${themeState.theme.quarkusRed};}.cls-3{fill:${themeState.theme.quarkusCenter};}</style></defs><title>Quarkus</title><polygon class="cls-1" points="669.34 180.57 512 271.41 669.34 362.25 669.34 180.57"/><polygon class="cls-2" points="354.66 180.57 354.66 362.25 512 271.41 354.66 180.57"/><polygon class="cls-3" points="669.34 362.25 512 271.41 354.66 362.25 512 453.09 669.34 362.25"/><polygon class="cls-1" points="188.76 467.93 346.1 558.76 346.1 377.09 188.76 467.93"/><polygon class="cls-2" points="346.1 740.44 503.43 649.6 346.1 558.76 346.1 740.44"/><polygon class="cls-3" points="346.1 377.09 346.1 558.76 503.43 649.6 503.43 467.93 346.1 377.09"/><polygon class="cls-1" points="677.9 740.44 677.9 558.76 520.57 649.6 677.9 740.44"/><polygon class="cls-2" points="835.24 467.93 677.9 377.09 677.9 558.76 835.24 467.93"/><polygon class="cls-3" points="520.57 649.6 677.9 558.76 677.9 377.09 520.57 467.93 520.57 649.6"/><path class="cls-1" d="M853.47,1H170.53C77.29,1,1,77.29,1,170.53V853.47C1,946.71,77.29,1023,170.53,1023h467.7L512,716.39,420.42,910H170.53C139.9,910,114,884.1,114,853.47V170.53C114,139.9,139.9,114,170.53,114H853.47C884.1,114,910,139.9,910,170.53V853.47C910,884.1,884.1,910,853.47,910H705.28l46.52,113H853.47c93.24,0,169.53-76.29,169.53-169.53V170.53C1023,77.29,946.71,1,853.47,1Z"/></svg>
                    <span class="logo-text" @click="${this._reload}">Dev UI</span>
                </div>
                <span class="title">${this._title}</span>
            </div>
            <div class="right-bar">
                ${this._rightSideNav}
                ${this._renderThemeOptions()}
            </div>
        </div>
        `;
    }

    _renderThemeOptions(){
        return html`<vaadin-menu-bar theme="tertiary-inline" class="themeDropdown" 
                        .items="${this._themeOptions}"
                        @item-selected="${(e) => this._changeThemeOption(e)}">
                    </vaadin-menu-bar>`;
    }

    _changeThemeOption(e){
        this._selectedTheme = e.detail.value.name;
        this._createThemeOptions();
        this._changeToSelectedTheme();
        this.storageControl.set('theme-preference', this._selectedTheme);
    }

    _changeToSelectedTheme(){
        if(this._selectedTheme === "desktop"){
            themeState.changeTo(this._desktopTheme);
        }else {
            themeState.changeTo(this._selectedTheme);
        }
    }

    _createThemeOptions(){

        let selectedComponent = this._desktopThemeItem;
        if(this._selectedTheme === "dark"){
            selectedComponent = this._darkThemeItem;
        }else if(this._selectedTheme === "light"){
            selectedComponent = this._lightThemeItem;
        }

        this._themeOptions = [
            {
                component: selectedComponent,
                children: [
                    {
                        component: this._darkThemeItem,
                        name: "dark"
                    },
                    {
                        component: this._lightThemeItem,
                        name: "light"
                    },
                    {
                        component: this._desktopThemeItem,
                        name: "desktop"
                    }
              ]
            }
            
          ];
    }

    _createThemeItems() {
        this._darkThemeItem = this._createThemeItem("moon", "dark");
        this._lightThemeItem = this._createThemeItem("sun", "light");
        this._desktopThemeItem = this._createThemeItem("desktop", "desktop");
      }

    _createThemeItem(iconName, ariaLabel) {
        const item = document.createElement('vaadin-context-menu-item');
        const icon = document.createElement('vaadin-icon');
        item.setAttribute('aria-label', ariaLabel);
        icon.setAttribute('icon', `font-awesome-solid:${iconName}`);
        item.appendChild(icon);
        return item;
    }

    _toggleTheme(){
        themeState.toggle();
    }

    _updateHeader(event){
        this._title = this.routerController.getCurrentTitle();
        var subMenu = this.routerController.getCurrentSubMenu();
        if(subMenu){
            this._rightSideNav = html`<vaadin-tabs selected="${subMenu.index}">
                                    ${subMenu.links.map(link =>
                                        html`${this._renderTab(subMenu.index, link)}`
                                    )}
                                </vaadin-tabs>`;
        }else{
            this._rightSideNav = html`
                <div class="app-info">
                    ${devuiState.applicationInfo.applicationName} ${devuiState.applicationInfo.applicationVersion}
                    <vaadin-button theme="secondary" @click="${this._previousDevUI}">Back to the previous Dev UI</vaadin-button>
                </div>`;
        }
    }

    _renderTab(index, link){
        if(!link.page.embed && link.page.includeInSubMenu){
            return html`
                ${this._renderSubMenuLink(index, link)}
                `;
        }else{
            return html`<vaadin-tab>
                ${this._renderSubMenuLink(index, link)}
            </vaadin-tab>`;
        }
    }

    _renderSubMenuLink(index, link){

        let relativePath = link.page.id.replace(link.page.namespace + "/", ""); 

        return html`<qwc-extension-link
            namespace="${link.page.namespace}"
            extensionName="${link.page.extensionId}"
            iconName="${link.page.icon}"
            displayName="${link.page.title}"
            staticLabel="${link.page.staticLabel}"
            dynamicLabel="${link.page.dynamicLabel}"
            streamingLabel="${link.page.streamingLabel}"
            path="${relativePath}"
            ?embed=${link.page.embed}
            externalUrl="${link.page.metadata.externalUrl}"
            webcomponent="${link.page.componentLink}" >
        </qwc-extension-link>`;
    }

    _reload(e) {
        fetch(devuiState.applicationInfo.contextRoot).then(response => {
            this.routerController.goHome();
        })
        .catch(error => {
            this.routerController.goHome();
            notifier.showErrorMessage("Seems like your server is not available.  <br/>Error : <code>" + error + "</code>","middle");
        });
    }

    _previousDevUI(){
        let pdui = this.routerController.getBasePath().replace("/dev-ui", "/dev-v1");;
        window.open(pdui, "_self").focus();
    }
}
customElements.define('qwc-header', QwcHeader);