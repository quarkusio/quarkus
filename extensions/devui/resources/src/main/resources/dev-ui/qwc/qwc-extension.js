import { LitElement, html, css} from 'lit';
import { observeState } from 'lit-element-state';
import '@vaadin/icon';
import '@vaadin/dialog';
import { dialogHeaderRenderer, dialogRenderer } from '@vaadin/dialog/lit.js';
import '@qomponent/qui-badge';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';
import { connectionState } from 'connection-state';

/**
 * This component represent one extension
 * It's a card on the extension board
 */
export class QwcExtension extends observeState(LitElement) {
    jsonRpc = new JsonRpc("devui-extensions", false);

    static styles = css`
        .card {
            height: 100%;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: 4px;
            width: 300px;
            filter: brightness(90%);
        }

        .card-header {
            font-size: var(--lumo-font-size-l);
            line-height: 1;
            height: 25px;
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            padding: 10px 10px;
            background-color: var(--lumo-contrast-5pct);
            border-bottom: 1px solid var(--lumo-contrast-10pct);
        }

        .card-footer {
            height: 20px;
            padding: 10px 10px;
            color: var(--lumo-contrast-50pct);
            display: flex;
            flex-direction: row;
            justify-content: space-between;
        }

        .card-footer a {
            color: var(--lumo-contrast-50pct);
        }

        .active:hover {
            box-shadow: 0 4px 8px 0 rgba(0,0,0,0.2);
        }

        .active .card-header{
            color: var(--lumo-contrast-80pct);
        }

        .inactive .card-header{
            color: var(--lumo-contrast-70pct);
        }
        
        .active:hover .config, .active:hover .more, .active:hover .guide, .active:hover .fav {
            visibility:visible;
        }

        .inactive:hover .config, .inactive:hover .more, .inactive:hover .guide {
            visibility:visible;
        }
    
        .guide, .more, .config, .fav {
            visibility:hidden;
        }

        .icon {
            font-size: x-small;
            cursor: pointer;
        }
        `;
    
    static properties = {
        _dialogOpened: {state: true},
        name: {type: String},
        namespace: {type: String},
        description: {type: String},
        guide: {type: String},
        clazz: {type: String},
        artifact: {type: String},
        shortName: {type: String},
        keywords: {},
        status: {type: String},
        configFilter: {},
        categories: {},
        unlisted: {type: String},
        builtWith: {type: String},
        providesCapabilities: {},
        extensionDependencies: {}, 
        favourite: {type: Boolean},
        installed: {type: Boolean},
        logoUrl: {type: String}
    };
    
    constructor() {
        super();
        this._dialogOpened = false;
        this.favourite = false;
        this.installed = false;
        this.logoUrl = null;
    }

    render() {
        
        return html`
            <vaadin-dialog class="detailDialog"
                header-title="${this.name} extension details"
                .opened="${this._dialogOpened}"
                @opened-changed="${(e) => (this._dialogOpened = e.detail.value)}"
                ${dialogHeaderRenderer(
                  () => html`
                    <vaadin-button theme="tertiary" @click="${() => (this._dialogOpened = false)}">
                        <vaadin-icon icon="font-awesome-solid:xmark"></vaadin-icon>
                    </vaadin-button>
                  `,
                  []
                )}
                ${dialogRenderer(() => this._renderDialog(), this.name)}
            ></vaadin-dialog>

            <div class="card ${this.clazz}">
                ${this._headerTemplate()}
                <slot name="content"></slot>
                ${this._footerTemplate()}
            </div>`;
    }

    _headerTemplate() {
        return html`<div class="card-header">
                        <div>${this.name}</div>
                        ${this._headerToolBar()}
                    </div>
          `;
    }

    _headerToolBar(){
        let favouriteIcon = "font-awesome-regular:star";
        let favouriteTitle = "Favour this extension";
        if(this.favourite){
            favouriteIcon = "font-awesome-solid:star";
            favouriteTitle = "Unfavour this extension";
        }

        return html`<div class="headerTools">
                        ${this.clazz == "active"?
                            html`<vaadin-icon class="icon fav" icon="${favouriteIcon}" @click="${this._fav}" title="${favouriteTitle}"></vaadin-icon>`:
                            html``
                        }
                        ${this.guide?
                            html`<vaadin-icon class="icon guide" icon="font-awesome-solid:book" @click="${this._guide}" title="Go to the ${this.name} guide"></vaadin-icon>`:
                            html``
                        }
                    </div>`;
    }

    _footerTemplate() {
        return html`
            <div class="card-footer">
                ${this._renderConfigFilterIcon()}
                ${this._renderStatus()}
                <vaadin-icon class="icon more" icon="font-awesome-solid:ellipsis-vertical" @click="${() => (this._dialogOpened = true)}" title="More about the ${this.name} extension"></vaadin-icon>
            </div>
        `;
    }

    _renderConfigFilterIcon(){
        if(this.configFilter){
            return html`<a href="configuration-form-editor?filter=${this.configFilter}" class="config">
                    <vaadin-icon class="icon" icon="font-awesome-solid:pen-to-square" title="Configuration for the ${this.name} extension"></vaadin-icon>
                </a>`;
        }else{
            return html`<span></span>`;
        }
    }

    _renderStatus(){
        var l = this._statusLevelOnCard();
        
        if(l) {
            return html`<qui-badge level="${l}" small><span>${this.status.toUpperCase()}</span></qui-badge>`;
        }
    }

    _statusLevelOnCard(){
        if(this.status === "experimental") {
            return "warning";
        } else if(this.status === "preview") {
            return "contrast";   
        }
        return null;
    }

    _statusLevel(){
        if(this.status === "stable") {
            return "success";
        }
        return this._statusLevelOnCard();
    }

    _renderDialog(){
        return html`
            ${this._renderLogo()}
            <table>
                <tr>
                    <td><b>Name</b></td>
                    <td>${this.name}</td>
                </tr>
                <tr>
                    <td><b>Namespace</b></td>
                    <td>${this.namespace}</td>
                </tr>
                <tr>
                    <td><b>Description</b></td>
                    <td>${this.description}</td>
                </tr>
                <tr>
                    <td><b>Guide</b></td>
                    <td>${this._renderGuideDetails()}</td>
                </tr>
                <tr>
                    <td><b>Artifact</b></td>
                    <td>${this._renderArtifact()}</td>
                </tr>
                <tr>
                    <td><b>Short name</b></td>
                    <td>${this.shortName}</td>
                </tr>        
                <tr>
                    <td><b>Keywords</b></td>
                    <td>${this._renderKeywordsDetails()}</td>
                </tr>        
                <tr>
                    <td><b>Status</b></td>
                    <td><qui-badge level="${this._statusLevel()}" small><span>${this.status.toUpperCase()}</span></qui-badge></td>
                </tr>        
                <tr>
                    <td><b>Config Filter</b></td>
                    <td>${this.configFilter}</td>
                </tr>
                <tr>
                    <td><b>Categories</b></td>
                    <td>${this.categories}</td>
                </tr>
                <tr>
                    <td><b>Unlisted</b></td>
                    <td>${this.unlisted}</td>
                </tr>
                <tr>
                    <td><b>Built with</b></td>
                    <td>${this.builtWith}</td>
                </tr>
                <tr>
                    <td><b>Provides capabilities</b></td>
                    <td>${this.providesCapabilities}</td>
                </tr>
                <tr>
                    <td><b>Extension dependencies</b></td>
                    <td>${this._renderExtensionDependencies()}</td>
                </tr>
            </table>
            ${this._renderUninstallButton()}
        `;
    }

    _renderLogo(){
        if(this.logoUrl){
            return html`<img style="position: absolute;right: 10px;" src="${this.logoUrl}" height="45" @error="${(e) => e.target.style.display = 'none'}">`;
        }
    }

    _renderUninstallButton(){
        if(connectionState.current.isConnected && this.installed){
            return html`<vaadin-button style="width: 100%;" theme="secondary error" @click="${this._uninstall}">
                        <vaadin-icon icon="font-awesome-solid:trash-can" slot="prefix"></vaadin-icon>
                        Remove this extension
                    </vaadin-button>`;
        }
    }

    _uninstall(){
        this._dialogOpened = false;
        notifier.showInfoMessage(this.name + " removal in progress");
        this.jsonRpc.removeExtension({extensionArtifactId:this.artifact}).then(jsonRpcResponse => {
            let outcome = jsonRpcResponse.result;
            if(!outcome){
                notifier.showErrorMessage(name + " removal failed");
            }
        });
    }

    _renderGuideDetails() {
        return this.guide
          ? html`<span style="cursor:pointer" @click=${this._guide}>${this.guide}</span>`
          : html``;
    }

    _renderKeywordsDetails() {
        return this._renderCommaString(this.keywords);
    }

    _renderExtensionDependencies() {
        return this._renderCommaString(this.extensionDependencies);
    }

    _renderArtifact(){
        if(this.artifact){
            return html`<code>${this.artifact}</code>`;
        }else{
            return html``;
        }
    }

    _renderCommaString(cs){
        if(cs) {
            var arr = cs.split(',');
            return html`<ul>${arr.map(v => 
                html`<li>${v}</li>`
            )}</ul>`;
        }else{
            return html``;
        }
    }

    _guide(e) {
        window.open(this.guide, '_blank').focus();
    }

    _fav(e){
        const name = this.namespace;
        if (name) {
          const options = {
            detail: {name},
            bubbles: true,
            composed: true,
          };
          this.dispatchEvent(new CustomEvent('favourite', options));
        }
    }
}

customElements.define('qwc-extension', QwcExtension);