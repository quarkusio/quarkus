import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/text-field';
import '@vaadin/icon';
import '@vaadin/progress-bar';
import '@vaadin/horizontal-layout';
import '@vaadin/combo-box';
import '@vaadin/details';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@qomponent/qui-badge';
import { gridRowDetailsRenderer } from '@vaadin/grid/lit.js';

/**
 * This component create the add extensions screen
 */
export class QwcExtensionAdd extends QwcHotReloadElement {
    jsonRpc = new JsonRpc("devui-extensions", false);
    
    static styles = css`
        .dialogContent {
            display: flex;
            width: 75vw;
            height: 80vh;
            flex-direction: column;
        }
    
        .grid {
            width: 100%;
            height: 100%;
        }
    
        .descriptionPane{
            display: flex;
            padding-left: 30px;
            padding-right: 30px;
            padding-top: 10px;
            padding-bottom: 10px;
            justify-content: space-between;
            gap: 10px;
        }
    
        .description {
            display: flex;
            flex-direction: column;
            color: var(--lumo-contrast-60pct);
            width: 100%;
        }
    
        a, a:visited {
            text-decoration: none;
            color: var(--lumo-contrast-60pct);
        }
        a:hover {
            color: var(--lumo-primary-color);
        }
        
        .line{
            padding-bottom: 3px;
        }
        .list{
            display: flex;
            align-items: center;
        }
        .install {
            position: absolute;
            top: 20px;
            right: 20px;
        }
        .filterbar {
            display: flex;
            gap: 10px;
            justify-content: space-between;
        }
        .loading {
            display: flex;
            flex-direction: column;
            gap: 20px;
            padding: 20px;
        }
    `;

    static properties = {
        _extensions: {state: true, type: Array},
        _categories: {state: true, type: Array},
        _filteredExtensions: {state: true, type: Array},
        _filteredValue: {state: true},
        _filteredCategory: {state: true},
        _detailsOpenedItem: {state: true, type: Array},
    }

    constructor() {
        super();
        this._extensions = null;
        this._categories = null;
        this._filteredExtensions = null;
        this._filteredValue = null;
        this._filteredCategory = null;
        this._detailsOpenedItem = [];
    }

    connectedCallback() {
        super.connectedCallback();
        
        this.jsonRpc.getCategories().then(jsonRpcResponse => {
            this._categories = jsonRpcResponse.result;
            this._categories.push({name:'Uncategorised', id: 'uncategorised'});
        });
        
        if(!this._extensions){
            this.hotReload();
        }
    }

    hotReload(){
        this.jsonRpc.getInstallableExtensions().then(jsonRpcResponse => {
            this._extensions = jsonRpcResponse.result;
            this._filteredExtensions = this._extensions;
        });
    }

    render() {
        if(this._filteredExtensions){
            return html`<div class="dialogContent">
                            ${this._renderFilterBar()}
                            ${this._renderGrid()}
                        </div>`;
        }else{
            return html`<div class="loading">
                            <vaadin-horizontal-layout style="justify-content: space-between;">
                                <label class="text-secondary" id="pblabel">Loading installable extensions</label>
                            </vaadin-horizontal-layout>
                            <vaadin-progress-bar class="progress" aria-labelledby="pblabel" indeterminate></vaadin-progress-bar>
                        </div>`;
        }
    }

    _renderGrid(){
        return html`<vaadin-grid .items="${this._filteredExtensions}" class="grid" theme="row-stripes"
                        .detailsOpenedItems="${this._detailsOpenedItem}"
                            @active-item-changed="${(event) => {
                                const prop = event.detail.value;
                                this._detailsOpenedItem = prop ? [prop] : [];
                            }}"
                            ${gridRowDetailsRenderer(this._descriptionRenderer, [])}>
                            <vaadin-grid-sort-column header="Name" path="name" auto-width flex-grow="0" resizable></vaadin-grid-sort-column>
                            <vaadin-grid-sort-column header="Description" path="description" resizable></vaadin-grid-sort-column>
                        </vaadin-grid>`;
    }

    _renderFilterBar(){
        return html`<div class="filterbar">
                        <vaadin-text-field style="width: 100%;"
                                placeholder="Filter"
                                value="${this._filteredValue}"
                                @value-changed="${(e) => this._filterTextChanged(e)}">
                            <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
                            <qui-badge slot="suffix"><span>${this._filteredExtensions.length}</span></qui-badge>
                        </vaadin-text-field>
                        ${this._renderCategoryDropdown()}
                    </div>`;
        
    }

    _renderCategoryDropdown(){
        if(this._categories){
            return html`<vaadin-combo-box
                            placeholder="Category"
                            item-label-path="name"
                            item-value-path="id"
                            .items="${this._categories}"
                            @value-changed="${(e) => this._filterCategoryChanged(e)}"
                            clear-button-visible
                        ></vaadin-combo-box>`;
        }
    }

    _filterCategoryChanged(e){
        this._filteredCategory = (e.detail.value || '').trim();
        return this._filterGrid();
    }

    _filterTextChanged(e) {
        this._filteredValue = (e.detail.value || '').trim();
        return this._filterGrid();
    }

    _filterGrid(){
        this._filteredExtensions = this._extensions.filter((prop) => {
            if(this._filteredValue && this._filteredValue !== '' && this._filteredCategory && this._filteredCategory !== ''){
                return this._filterByTerm(prop) && this._filterByCategory(prop);
            }else if(this._filteredValue && this._filteredValue !== ''){
                 return this._filterByTerm(prop);
            }else if(this._filteredCategory && this._filteredCategory !== ''){
                return this._filterByCategory(prop);
            }else{
                return true;
            }
        });
    }

    _filterByTerm(prop){
        if(prop.metadata && prop.metadata.keywords){
            return this._match(prop.name, this._filteredValue) || this._match(prop.description, this._filteredValue) || prop.metadata.keywords.includes(this._filteredValue);
        }else{
            return this._match(prop.name, this._filteredValue) || this._match(prop.description, this._filteredValue);
        }
    }

    _filterByCategory(prop){
        if(prop.metadata && prop.metadata.categories){
            return prop.metadata.categories.includes(this._filteredCategory);
        }else if(this._filteredCategory === "uncategorised"){
            return true;
        }else {
            return false;
        }
    }

    _match(value, term) {
        if (! value) {
            return false;
        }
        
        return value.toLowerCase().includes(term.toLowerCase());
    }

    _descriptionRenderer(prop) {
        
        return html`<div class="descriptionPane">
                        <div class="description">
                            <span class="line"><b>Artifact:</b> ${prop.artifact.groupId}:${prop.artifact.artifactId}</span>
                            <span class="line"><b>Version:</b> ${prop.artifact.version}</span>
                            ${this._renderIsPlatform(prop)}
                            ${this._renderMetadata1(prop)}
                        </div>
                        <div class="description">
                            ${this._renderMetadata2(prop)}
                        </div>
                    </div>
                    <vaadin-button class="install" theme="secondary success" @click="${() => this._install(prop)}">
                        <vaadin-icon icon="font-awesome-solid:download" slot="prefix"></vaadin-icon>
                        Add Extension
                    </vaadin-button>    
                    `;
    }

    _renderMetadata1(prop){
        if(prop.metadata){
            return html`${this._renderGuide(prop.metadata)}
                        ${this._renderScmUrl(prop.metadata)}
                        ${this._renderStatus(prop.metadata)}
                        ${this._renderMinJavaVersion(prop.metadata)}`;
        }
    }

    _renderIsPlatform(prop){
        if (prop.origins && prop.origins.some(str => str.startsWith("io.quarkus:quarkus-bom-quarkus-platform"))){
            return html`<span class="line"><b>Platform:</b> <vaadin-icon style="height: var(--lumo-icon-size-s); width: var(--lumo-icon-size-s);" icon="font-awesome-solid:check"></vaadin-icon></span>`;
        } else {
            return html`<span class="line"><b>Platform:</b> <vaadin-icon style="height: var(--lumo-icon-size-s); width: var(--lumo-icon-size-s);" icon="font-awesome-solid:xmark"></vaadin-icon></span>`;
        } 
    }

    _renderGuide(metadata){
       if (metadata.guide){
           return html`<span class="line"><b>Guide:</b> <a target="_blank" href="${metadata.guide}">${metadata.guide}</a></span>`;
        } 
    }

    _renderScmUrl(metadata){
        if (metadata['scm-url']){
           return html`<span class="line"><b>SCM:</b> <a target="_blank" href="${metadata['scm-url']}">${metadata['scm-url']}</a></span>`;     
        }
    }
    
    _renderStatus(metadata){
        if(metadata.status){
            return html`<span class="line"><b>Status:</b> <qui-badge level="${this._statusLevel(metadata.status)}" small><span>${metadata.status.toUpperCase()}</span></qui-badge></span>`;     
        }
    }
    
    _renderMinJavaVersion(metadata){
        if(metadata['minimum-java-version']){
           return html`<span class="line"><b>Minimum Java version:</b> ${metadata['minimum-java-version']}</span>`;     
        }
    }

    _renderMetadata2(prop){
        if(prop.metadata){
            return html`${this._renderKeywords(prop.metadata)}
                        ${this._renderCategories(prop.metadata)}
                        ${this._renderExtensionDependencies(prop.metadata)}`;
        }
    }

    

    
    
    _statusLevel(s){
        if(s === "stable") {
            return "success";
        } else if(s === "experimental") {
            return "warning";
        } else if(s === "preview") {
            return "contrast";   
        }
        return null;
    }
    
    _renderCategories(metadata){
        if(metadata.categories){
           return this._renderList("Categories", metadata.categories);
        }
    }
    
    _renderKeywords(metadata){
        if(metadata.keywords){
           return this._renderList("Keywords", metadata.keywords);
        }
    }
    
    _renderExtensionDependencies(metadata){
        if(metadata['extension-dependencies']){
           return html`<vaadin-details summary="Extension dependencies">
                            <vaadin-vertical-layout>
                                ${this._renderExtensionDependenciesLines(metadata['extension-dependencies'])}
                            </vaadin-vertical-layout>
                        </vaadin-details>`;     
        }
    }
    
    _renderExtensionDependenciesLines(lines){
        return html`
            ${lines.map((line) =>
                html`<span>${line}</span>`
            )}
        `;
    }
    
    _renderList(heading, list) {
        return html`<span class="list"><b>${heading}:</b> ${this._renderListLines(list)}</span>`;
    }
    
    _renderListLines(list) {
        return html`
          <ul>
            ${list.map((item) =>
                html`<li>${item}</li>`
            )}
          </ul>
        `;
    }
    
    _install(prop){
        let extensionArtifactId = prop.artifact.groupId + ':' + prop.artifact.artifactId;
        this.jsonRpc.addExtension({extensionArtifactId:extensionArtifactId}).then(jsonRpcResponse => {
            let outcome = jsonRpcResponse.result;
            
            const options = {
                detail: {outcome: outcome, name: prop.name},
                bubbles: true,
                composed: true,
            };
            this.dispatchEvent(new CustomEvent('inprogress', options));
            
        });   
    }
}
customElements.define('qwc-extension-add', QwcExtensionAdd);
