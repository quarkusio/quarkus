import { LitElement, html, css} from 'lit';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { RouterController } from 'router-controller';
import { devuiState } from 'devui-state';
import { observeState } from 'lit-element-state';
import { allowExtensionManagement } from 'devui-data';
import 'qwc/qwc-extension.js';
import 'qwc/qwc-extension-link.js';
import 'qwc/qwc-extension-add.js';
import { StorageController } from 'storage-controller';
import '@vaadin/dialog';
import { dialogHeaderRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import { notifier } from 'notifier';
import { connectionState } from 'connection-state';
import { JsonRpc } from 'jsonrpc';

/**
 * This component create cards of all the extensions
 */
export class QwcExtensions extends observeState(LitElement) {
    routerController = new RouterController(this);
    storageController = new StorageController(this);
    jsonRpc = new JsonRpc("devui-extensions", false);
    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            max-height: 100%;
        }
        .grid {
            display: flex;
            flex-wrap: wrap;
            gap: 20px;
            padding-left: 5px;
            padding-right: 10px;
        }
        
        .description {
            padding-bottom: 10px;
            color: var(--lumo-contrast-50pct);
        }
    
        .card-content {
            color: var(--lumo-contrast-90pct);
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            padding: 10px 10px;
            height: 100%;
        }

        .card-content slot {
            display: flex;
            flex-flow: column wrap;
            padding-top: 5px;
        }
        .float-right {
            align-self: flex-end;
        }
    
        qwc-extension-link {
            cursor: grab;
        }
        .addExtensionButton {
            position: absolute;
            bottom: 40px;
            right: 40px;
            width: 3em;
            height: 3em;
            box-shadow: var(--lumo-shade) 5px 5px 15px 3px;
            z-index: 9;
        }
        .addExtensionIcon {
            width: 2em;
            height: 2em;
        }
       `;

    static properties = {
        _favourites: {state: true},
        _addDialogOpened: {state: true},
        _installedExtensions: {state: true, type: Array},
        _selectedFilters: {state: true, type: Array},
        _addExtensionsEnabled: {state: true, type: Boolean},
    }

    constructor() {
        super();
        this._favourites = this._getStoredFavourites();
        this._addDialogOpened = false;
        this._installedExtensions = [];
        this._selectedFilters = ["Favorites","Active","Inactive"];
        this._addExtensionsEnabled = false;
    }

    connectedCallback() {
        super.connectedCallback();
        window.addEventListener('extensions-filters-changed', this._onFiltersChanged);
        if (allowExtensionManagement) {
            this.jsonRpc.getInstalledNamespaces().then(jsonRpcResponse => {
                if (jsonRpcResponse.result) {
                    this._installedExtensions = jsonRpcResponse.result;
                    this._addExtensionsEnabled = true;
                }
            }).catch(e => {
                notifier.showErrorMessage("Could not list namespaces "+ e?.error?.message);
            });
        }
    }

    disconnectedCallback() {
        window.removeEventListener('extensions-filters-changed', this._onFiltersChanged);
        super.disconnectedCallback();
    }

    render() {
        return html`
            ${this._renderGrid()}
            ${this._renderAddDialog()}`;
    }

    _renderGrid(){
        return html`<div class="grid">
            ${this._renderActives(devuiState.cards.active)}
            ${this._renderInactives(devuiState.cards.inactive)}
        </div>`;
    }

    _renderActives(extensions){
        let favouriteExtensions = [];
        let unfavouriteExtensions = [];

        for (let i = 0; i < extensions.length; i++) {
            let extension = extensions[i];
            // Make sure we import the components
            extension.cardPages.forEach(page => {
                if(page.embed){ // we need to register with the router
                    import(page.componentRef);
                    this.routerController.addRouteForExtension(page);
                }else if(page.includeInMenu){ // we need to add the link to the submenu
                    this.routerController.addExternalLink(page);
                }
            });
            
            if(this._favourites.includes(extension.namespace)){
                favouriteExtensions.push(extension);
            } else {
                unfavouriteExtensions.push(extension);
            }
        }
        
        return html`
            ${this._renderFavourites(favouriteExtensions)}
            ${this._renderUnfavourites(unfavouriteExtensions)}
        `;
    }

    _renderFavourites(favouriteExtensions){
        if (this._selectedFilters.includes("Favorites")) {
            return html`
                ${favouriteExtensions.map(e => this._renderActive(e,true))}
            `;
        }
    }

    _renderUnfavourites(unfavouriteExtensions){
        if (this._selectedFilters.includes("Active")) {
            return html`
                ${unfavouriteExtensions.map(e => this._renderActive(e,false))}
            `;
        }
    }

    _renderActive(extension, fav){
        return html`
                <qwc-extension 
                    clazz="active"
                    name="${extension.name}" 
                    description="${extension.description}"
                    guide="${extension.guide}"
                    namespace="${extension.namespace}"
                    artifact="${extension.artifact}"
                    shortName="${extension.shortName}"
                    keywords="${extension.keywords}"
                    status="${extension.status}"
                    configFilter="${extension.configFilter}"
                    categories="${extension.categories}"
                    unlisted="${extension.unlisted}"
                    builtWith="${extension.builtWith}"
                    providesCapabilities="${extension.providesCapabilities}"
                    extensionDependencies="${extension.extensionDependencies}"
                    ?installed=${this._installedExtensions.includes(extension.namespace)}
                    ?favourite=${fav} 
                    @favourite=${this._favourite}>

                    ${this._renderCardContent(extension)}

                </qwc-extension>

            `;
    }

    _renderInactives(extensions){
        if (this._selectedFilters.includes("Inactive")) {
            return html`
                ${extensions.map(extension => this._renderInactive(extension))}
            `; 
        }
    }

    _renderInactive(extension){
        if(extension.unlisted === "false"){
            return html`<qwc-extension
                clazz="inactive"
                name="${extension.name}" 
                description="${extension.description}" 
                guide="${extension.guide}"
                namespace="${extension.namespace}"
                artifact="${extension.artifact}"
                shortName="${extension.shortName}"
                keywords="${extension.keywords}"
                status="${extension.status}"
                configFilter="${extension.configFilter}"
                categories="${extension.categories}"
                unlisted="${extension.unlisted}"
                builtWith="${extension.builtWith}"
                providesCapabilities="${extension.providesCapabilities}"
                extensionDependencies="${extension.extensionDependencies}">
                <div class="card-content" slot="content">
                    ${extension.description}
                </div>    
            </qwc-extension>`;
        }
    }

    _favourite(e){
        let favourites = this._getStoredFavourites();
        let extName = e.detail.name;
        if(favourites.includes(extName)){
            const index = favourites.indexOf(extName);
            if (index > -1) {
                favourites.splice(index, 1);
            }
        }else{
            favourites.push(extName);
        }
        this._setStoredFavourites(favourites);

        this._favourites = this._getStoredFavourites();
    }

    _getStoredFavourites(){
        let favourites = this.storageController.get('favourites');
        if(favourites){
            return JSON.parse(favourites);
        }else{
            return [];
        }
    }

    _setStoredFavourites(favourites){
        this.storageController.set('favourites', JSON.stringify(favourites));
    }

    _renderCardContent(extension){
        if(extension.card){
            return this._renderCustomCardContent(extension);
        } else {
            return this._renderDefaultCardContent(extension);
        }
    }

    _renderCustomCardContent(extension){
        import(extension.card.componentRef);
        let customCardCode = `<${extension.card.componentName} 
                                class="card-content"
                                slot="content"
                                extensionName="${extension.name}"
                                description="${extension.description}"
                                guide="${extension.guide}"
                                namespace="${extension.namespace}">

                             </${extension.card.componentName}>`;

        return html`${unsafeHTML(customCardCode)}`;

    }

    _renderDefaultCardContent(extension){
        return html`
            <div class="card-content" slot="content">
                <span class="description">
                    ${extension.description}
                </span>
                ${this._renderCardLinks(extension)}
            </div>`;
    }

    _renderCardLinks(extension){

        return html`${extension.cardPages.map(page => html`
                            <qwc-extension-link slot="link"
                                namespace="${extension.namespace}"
                                extensionName="${extension.name}"
                                iconName="${page.icon}"
                                tooltipContent="${page.tooltip}"
                                colorName="${page.color}"
                                displayName="${page.title}"
                                staticLabel="${page.staticLabel}"
                                dynamicLabel="${page.dynamicLabel}"
                                streamingLabel="${page.streamingLabel}"
                                path="${page.id}"
                                ?embed=${page.embed}
                                externalUrl="${page.metadata.externalUrl}"
                                dynamicUrlMethodName="${page.metadata.dynamicUrlMethodName}"
                                webcomponent="${page.componentLink}" 
                                draggable="true" @dragstart="${this._handleDragStart}">
                            </qwc-extension-link>
                        `)}`;
    }

    _handleDragStart(event) {
        const extensionNamespace = event.currentTarget.getAttribute('namespace');
        const pageId = event.currentTarget.getAttribute('path');

        const extension = devuiState.cards.active.find(obj => obj.namespace === extensionNamespace);
        const page = extension.cardPages.find(obj => obj.id === pageId);
        const jsonData = JSON.stringify(page);
        event.dataTransfer.setData('application/json', jsonData);
    }

    _renderAddDialog(){
        if (this._addExtensionsEnabled) {
            return html`
                <vaadin-dialog
                theme="no-padding"
                resizable
                draggable
                header-title="Add extension"
                .opened="${this._addDialogOpened}"
                @opened-changed="${(event) => {
                    this._addDialogOpened = event.detail.value;
                }}"
                ${dialogHeaderRenderer(
                    () => html`
                        <vaadin-button theme="tertiary" @click="${() => (this._addDialogOpened = false)}">
                            <vaadin-icon icon="font-awesome-solid:xmark"></vaadin-icon>
                        </vaadin-button>
                    `,
                    []
                    )}
                ${dialogRenderer(
                    () => html`<qwc-extension-add @inprogress="${this._installRequest}"></qwc-extension-add>`
                )}
                ></vaadin-dialog>
                ${this._renderAddExtensionButton()}
            `;
        }
    }
    
    _renderAddExtensionButton(){
        if(connectionState.current.isConnected){
            return html`<vaadin-button class="addExtensionButton" theme="icon" aria-label="Add Extension" title="Add Extension" @click="${this._openAddDialog}">
                        <vaadin-icon class="addExtensionIcon" icon="font-awesome-solid:plus"></vaadin-icon>
                    </vaadin-button>`;
        }
    }
    
    _installRequest(e){
        this._addDialogOpened = false;
        let name = e.detail.name;
        if(e.detail.outcome){
            notifier.showInfoMessage(name + " installation in progress");
        }else{
            notifier.showErrorMessage(name + " installation failed");
        }
    }
    
    _openAddDialog() {
        this._addDialogOpened = true;
    }

    _onFiltersChanged = (event) => {
        this._selectedFilters = event.detail.filters;
    }
}
customElements.define('qwc-extensions', QwcExtensions);
