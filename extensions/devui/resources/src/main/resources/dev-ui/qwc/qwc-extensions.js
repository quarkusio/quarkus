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
import { themeState } from 'theme-state';
import { JsonRpc } from 'jsonrpc';
import '@qomponent/qui-badge';
import { assistantState } from 'assistant-state';

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
            padding-bottom: 10px;
        }
        
        .description {
            padding-bottom: 10px;
            color: var(--lumo-contrast-50pct);
            display: flex;
            gap: 10px;
        }
    
        .libraryVersion {
            display: flex;
            justify-content: center;
            gap: 5px;
            flex-wrap: wrap;
        }
    
        .card-content {
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
            height: 100%;
            gap: 20px;
        }
    
        .card-content-top{
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
        _filterText: {state: true},
        _showFilterBar: {state: true}
    }

    constructor() {
        super();
        this._favourites = this._getStoredFavourites();
        this._addDialogOpened = false;
        this._installedExtensions = [];
        this._selectedFilters = ["Favorites","Active","Inactive"];
        this._addExtensionsEnabled = false;
        this._filterText = '';
        this._showFilterBar = false;
    }

    connectedCallback() {
        super.connectedCallback();
        window.addEventListener('extensions-filters-changed', this._onFiltersChanged);
        window.addEventListener('keydown', this._handleGlobalKeyDown);
        window.addEventListener('storage-changed', this._storageChange);
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
        window.removeEventListener('keydown', this._handleGlobalKeyDown);
        window.removeEventListener('storage-changed', this._storageChange);
        super.disconnectedCallback();
    }

    render() {
        return html`
            ${this._renderFilterbar()}
            ${this._renderGrid()}
            ${this._renderAddDialog()}`;
    }

    _renderFilterbar(){
        if(this._showFilterBar){
            return html`<div style="padding: 10px;">
                        <vaadin-text-field
                            id="extensionFilterInput"
                            theme="small"
                            style="width: 100%;"
                            placeholder="Filter extensions…"
                            clear-button-visible
                            .value=${this._filterText}
                            @input=${this._onFilterInput}>
                                <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
                        </vaadin-text-field>
                    </div>`;
        }
    }
    
    _onFilterInput(e) {
        const value = e.target.value.trim();
        this._filterText = value;

        if (!value) {
            this._showFilterBar = false;
        }
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
        if(!this._shouldFilter(extension)){
            let logoUrl = this._getLogoUrl(extension);
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
                    @favourite=${this._favourite}
                    logoUrl="${logoUrl}">

                    ${this._renderCardContent(extension, logoUrl)}

                </qwc-extension>

            `;
        }
    }

    _shouldFilter(extension){
        const filter = this._filterText?.toLowerCase() || '';

        const haystack = [
            extension.name,
            extension.description,
            extension.shortName,
            extension.keywords,
            extension.artifact,
            extension.namespace
        ].filter(Boolean).join(' ').toLowerCase();

        return filter && !haystack.includes(filter);
    }

    _renderInactives(extensions){
        if (this._selectedFilters.includes("Inactive")) {
            return html`
                ${extensions.map(extension => this._renderInactive(extension))}
            `; 
        }
    }

    _renderInactive(extension){
        if(extension.unlisted === "false" || extension.libraryLinks){
            if(!this._shouldFilter(extension)){
                let logoUrl = this._getLogoUrl(extension);

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
                    extensionDependencies="${extension.extensionDependencies}"
                    logoUrl="${logoUrl}">

                    ${this._renderCardContent(extension, logoUrl)}

                </qwc-extension>`;
            }
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

    _renderCardContent(extension, logoUrl){
        if(extension.card){
            return this._renderCustomCardContent(extension, logoUrl);
        } else {
            return this._renderDefaultCardContent(extension, logoUrl);
        }
    }

    _renderCustomCardContent(extension, logoUrl){
        import(extension.card.componentRef);
        let customCardCode = `<${extension.card.componentName} 
                                class="card-content-top"
                                slot="content"
                                extensionName="${extension.name}"
                                description="${extension.description}"
                                guide="${extension.guide}"
                                namespace="${extension.namespace}"
                                logoUrl="${logoUrl}">

                             </${extension.card.componentName}>`;

        return html`${unsafeHTML(customCardCode)}`;

    }

    _renderDefaultCardContent(extension, logo){
        return html`
            <div class="card-content" slot="content">
                <div class="card-content-top">
                    <span class="description">
                        ${this._renderLogo(logo)}
                        ${extension.description}
                    </span>
                    ${this._renderCardLinks(extension)}
                </div>
                ${this._renderLibraryVersions(extension)}
            </div>`;
    }

    _renderLogo(logo){
        if(logo){
            return html`<img src="${logo}" height="45" @error="${(e) => e.target.style.display = 'none'}">`;
        }
    }

    _getLogoUrl(extension){
        if(extension.darkLogo && themeState.theme.name === "dark"){
            return this._getThemedLogoUrl(extension, extension.darkLogo);
        }else if(extension.lightLogo){
            return this._getThemedLogoUrl(extension, extension.lightLogo);
        }
        return null;
    }

    _getThemedLogoUrl(extension, logoUrl){
        if(!logoUrl.startsWith("http://") && !logoUrl.startsWith("https://")){
            return "./" + extension.namespace + "/" + logoUrl;
        }
        return logoUrl;
    }

    _renderCardLinks(extension){
        return html`${extension.cardPages.map(page => html`${this._renderCardLink(extension, page)}`)}`;
    }

    _renderCardLink(extension, page){
        if(!page.assistantPage || assistantState.current.isConfigured){
            return html`<qwc-extension-link slot="link"
                                streamingLabelParams="${page.streamingLabelParams}"
                                webcomponentTagName="${page.componentName}"
                                namespace="${extension.namespace}"
                                extensionName="${extension.name}"
                                iconName="${page.icon}"
                                tooltipContent="${page.tooltip}"
                                colorName="${page.color}"
                                displayName="${page.title}"
                                path="${page.id}"
                                ?embed=${page.embed}
                                externalUrl="${page.metadata.externalUrl}"
                                dynamicUrlMethodName="${page.metadata.dynamicUrlMethodName}"
                                webcomponent="${page.componentLink}"
                                staticLabel="${page.staticLabel}" 
                                dynamicLabel="${page.dynamicLabel}"
                                streamingLabel="${page.streamingLabel}"
                                draggable="true" @dragstart="${this._handleDragStart}">
                            </qwc-extension-link>
                        `;
        }
    }

    _renderLibraryVersions(extension) {
        return html`
          <div class="libraryVersion">
            ${extension.libraryLinks?.map(libraryLink => html`
              <div
                style="cursor: ${libraryLink.url ? 'pointer' : 'default'}"
                @click=${libraryLink.url ? () => window.open(libraryLink.url, '_blank', 'noopener,noreferrer') : null}>
                <qui-badge small>
                  <span>${libraryLink.name} ${libraryLink.version}</span>
                </qui-badge>
              </div>
            `)}
          </div>
        `;
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

    _storageChange = (e) => {
        if(e.detail.method === "remove" && e.detail.key.startsWith("qwc-extensions-")){
            this._selectedFilters = ["Favorites","Active","Inactive"];
            this._favourites = this._getStoredFavourites();
        }
    }
    
    _handleGlobalKeyDown = (e) => {
        if (!this._showFilterBar && e.key.length === 1 && !e.metaKey && !e.ctrlKey && !e.altKey) {
            this._showFilterBar = true;
            const key = e.key;
            this.updateComplete.then(() => {
                const input = this.shadowRoot.getElementById('extensionFilterInput');
                if (input) {
                    input.value = key;
                    input.focus();
                    this._filterText = key;
                }
            });
            e.preventDefault(); // Stop the browser from processing the key further
        } else if (e.key === 'Escape' && this._showFilterBar) {
            this._filterText = '';
            this._showFilterBar = false;
        }
    }


    _onFiltersChanged = (event) => {
        this._selectedFilters = event.detail.filters;
    }
}
customElements.define('qwc-extensions', QwcExtensions);
