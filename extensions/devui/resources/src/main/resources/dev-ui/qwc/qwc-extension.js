import { LitElement, html, css} from 'lit';
import { observeState } from 'lit-element-state';
import '@vaadin/icon';
import '@vaadin/dialog';
import { dialogHeaderRenderer, dialogRenderer, dialogFooterRenderer } from '@vaadin/dialog/lit.js';
import '@qomponent/qui-badge';
import '@qomponent/qui-card';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';
import { connectionState } from 'connection-state';
import { msg, str, updateWhenLocaleChanges } from 'localization';

/**
 * This component represent one extension
 * It's a card on the extension board
 */
export class QwcExtension extends observeState(LitElement) {
    jsonRpc = new JsonRpc("devui-extensions", false);

    static styles = css`
        :host {
            width: 100%;
            height: 100%;
        }

        qui-card {
            height: 100%;
            transition: transform var(--devui-transition-normal, 0.2s ease),
                        box-shadow var(--devui-transition-normal, 0.2s ease);
        }

        :host(:hover) qui-card {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px var(--lumo-contrast-10pct);
        }

        .card-header {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            width: 100%;
            gap: 8px;
        }

        .card-header-left {
            display: flex;
            align-items: center;
            gap: 8px;
            min-width: 0;
        }

        .card-header-left img {
            border-radius: 4px;
            flex-shrink: 0;
        }

        .card-header-name {
            font-weight: 500;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .card-footer {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            width: 100%;
            padding: 10px;
            box-sizing: border-box;
            color: var(--lumo-contrast-40pct);
        }

        .card-footer a {
            color: var(--lumo-contrast-40pct);
        }

        :host(:hover) .config, :host(:hover) .more, :host(:hover) .guide, :host(:hover) .fav {
            visibility:visible;
            opacity: 1;
        }

        .guide, .more, .config, .fav {
            visibility:hidden;
            opacity: 0;
            --vaadin-icon-size: var(--lumo-font-size-m);
            transition: opacity var(--devui-transition-fast, 0.15s ease), color var(--devui-transition-fast, 0.15s ease);
        }

        .icon {
            font-size: x-small;
            cursor: pointer;
            color: var(--lumo-contrast-40pct);
        }

        .icon:hover {
            color: var(--lumo-primary-color);
        }

        .headerTools {
            display: flex;
            gap: 6px;
            flex-shrink: 0;
        }

        /* Status pill styling */
        .status-pill {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: var(--lumo-font-size-xxs);
            font-weight: 500;
            text-transform: uppercase;
            letter-spacing: 0.04em;
            padding: 2px 8px;
            border-radius: 12px;
        }

        .status-dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
        }

        .status-experimental {
            background-color: hsla(30, 100%, 50%, 0.12);
            color: var(--lumo-warning-text-color);
        }
        .status-experimental .status-dot {
            background-color: var(--lumo-warning-color);
        }

        .status-preview {
            background-color: var(--lumo-contrast-5pct);
            color: var(--lumo-contrast-60pct);
        }
        .status-preview .status-dot {
            background-color: var(--lumo-contrast-40pct);
        }

        .status-stable {
            background-color: hsla(145, 72%, 30%, 0.12);
            color: var(--lumo-success-text-color);
        }
        .status-stable .status-dot {
            background-color: var(--lumo-success-color);
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
        updateWhenLocaleChanges(this);
        this._dialogOpened = false;
        this.favourite = false;
        this.installed = false;
        this.logoUrl = null;
    }

    render() {
        const isActive = this.clazz === "active";
        const accent = isActive ? "var(--lumo-primary-color-10pct)" : null;

        return html`
            <vaadin-dialog class="detailDialog"
                header-title="${this.name} ${msg('extension details', { id: 'extensions-details' })}"
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
                ${dialogRenderer(() => this._renderDialog(), [])}
                ${dialogFooterRenderer(() => this._renderUninstallButton(), [this.installed, connectionState.current.isConnected])}
            ></vaadin-dialog>

            <qui-card actionable ?inactive=${!isActive} accent="${accent}"
                footer="${this._hasFooter() ? '' : null}">
                <div slot="header" class="card-header">
                    <div class="card-header-left">
                        ${this._renderHeaderLogo()}
                        <span class="card-header-name">${this.name}</span>
                    </div>
                    ${this._headerToolBar()}
                </div>
                <slot name="content" slot="content"></slot>
                ${this._renderFooter()}
            </qui-card>`;
    }

    _renderHeaderLogo(){
        if(this.logoUrl){
            return html`<img src="${this.logoUrl}" width="20" height="20" @error="${(e) => e.target.style.display = 'none'}">`;
        }
    }

    _hasFooter(){
        return true;
    }

    _renderFooter(){
        return html`<div slot="footer" class="card-footer">
            ${this._renderConfigFilterIcon()}
            ${this._renderStatus()}
            <vaadin-icon class="icon more" icon="font-awesome-solid:ellipsis" @click="${() => (this._dialogOpened = true)}" title="${msg(str`More about the ${this.name} extension`, { id: 'extensions-more' })}"></vaadin-icon>
        </div>`;
    }

    _headerToolBar(){
        let favouriteIcon = "font-awesome-regular:star";
        let favouriteTitle = msg('Favour this extension', { id: 'extensions-favour' });
        if(this.favourite){
            favouriteIcon = "font-awesome-solid:star";
            favouriteTitle = msg('Unfavour this extension', { id: 'extensions-unfavour' });
        }
        const name = this.name;
        return html`<div class="headerTools">
                        ${this.clazz === "active"?
                            html`<vaadin-icon class="icon fav" icon="${favouriteIcon}" @click="${this._fav}" title="${favouriteTitle}"></vaadin-icon>`:
                            html``
                        }
                        ${this.guide?
                            html`<vaadin-icon class="icon guide" icon="font-awesome-solid:book" @click="${this._guide}" title="${msg(str`Go to the ${name} guide`, { id: 'extensions-guide' })}"></vaadin-icon>`:
                            html``
                        }
                    </div>`;
    }

    _renderConfigFilterIcon(){
        const name = this.name;
        if(this.configFilter){
            return html`<a href="configuration-form-editor?filter=${this.configFilter}" class="config">
                    <vaadin-icon class="icon" icon="font-awesome-solid:pen-to-square" title="${msg(str`Configuration for the ${name} extension`, { id: 'extensions-config' })}"></vaadin-icon>
                </a>`;
        }else{
            return html`<span></span>`;
        }
    }

    _renderStatus(){
        if(this.status === "experimental") {
            return html`<span class="status-pill status-experimental"><span class="status-dot"></span>${this.status.toUpperCase()}</span>`;
        } else if(this.status === "preview") {
            return html`<span class="status-pill status-preview"><span class="status-dot"></span>${this.status.toUpperCase()}</span>`;
        }
        return null;
    }

    _statusLevel(){
        if(this.status === "stable") {
            return "success";
        } else if(this.status === "experimental") {
            return "warning";
        } else if(this.status === "preview") {
            return "contrast";
        }
        return null;
    }

    _renderDialog(){
        return html`
            ${this._renderDialogLogo()}
            <table>
                <tr>
                    <td><b>${msg('Name', { id: 'extensions-name' })}</b></td>
                    <td>${this.name}</td>
                </tr>
                <tr>
                    <td><b>${msg('Namespace', { id: 'extensions-namespace' })}</b></td>
                    <td>${this.namespace}</td>
                </tr>
                <tr>
                    <td><b>${msg('Description', { id: 'extensions-description' })}</b></td>
                    <td>${msg(this.description, {id: this.namespace + '-meta-description'})}</td>
                </tr>
                <tr>
                    <td><b>${msg('Guide', { id: 'extensions-guide-label' })}</b></td>
                    <td>${this._renderGuideDetails()}</td>
                </tr>
                <tr>
                    <td><b>${msg('Artifact', { id: 'extensions-artifact' })}</b></td>
                    <td>${this._renderArtifact()}</td>
                </tr>
                <tr>
                    <td><b>${msg('Short name', { id: 'extensions-short-name' })}</b></td>
                    <td>${this.shortName}</td>
                </tr>
                <tr>
                    <td><b>${msg('Keywords', { id: 'extensions-keywords' })}</b></td>
                    <td>${this._renderKeywordsDetails()}</td>
                </tr>
                <tr>
                    <td><b>${msg('Status', { id: 'extensions-status' })}</b></td>
                    <td>${this._renderStatus()}</td>
                </tr>
                <tr>
                    <td><b>${msg('Config Filter', { id: 'extensions-config-filter' })}</b></td>
                    <td>${this.configFilter}</td>
                </tr>
                <tr>
                    <td><b>${msg('Categories', { id: 'extensions-categories' })}</b></td>
                    <td>${this.categories}</td>
                </tr>
                <tr>
                    <td><b>${msg('Unlisted', { id: 'extensions-unlisted' })}</b></td>
                    <td>${this.unlisted}</td>
                </tr>
                <tr>
                    <td><b>${msg('Built with', { id: 'extensions-built-with' })}</b></td>
                    <td>${this.builtWith}</td>
                </tr>
                <tr>
                    <td><b>${msg('Provides capabilities', { id: 'extensions-capabilities' })}</b></td>
                    <td>${this.providesCapabilities}</td>
                </tr>
                <tr>
                    <td><b>${msg('Extension dependencies', { id: 'extensions-dependencies' })}</b></td>
                    <td>${this._renderExtensionDependencies()}</td>
                </tr>
            </table>
        `;
    }

    _renderDialogLogo(){
        if(this.logoUrl){
            return html`<img style="position: absolute;right: 10px;" src="${this.logoUrl}" height="45" @error="${(e) => e.target.style.display = 'none'}">`;
        }
    }

    _renderUninstallButton(){
        if(connectionState.current.isConnected && this.installed){
            return html`<vaadin-button style="width: 100%;" theme="secondary error" @click="${this._uninstall}">
                        <vaadin-icon icon="font-awesome-solid:trash-can" slot="prefix"></vaadin-icon>
                        ${msg('Remove this extension', { id: 'extensions-remove' })}
                    </vaadin-button>`;
        }
    }

    _uninstall(){
        this._dialogOpened = false;
        notifier.showInfoMessage(this.name + " " + msg('removal in progress', { id: 'extensions-removing' }));
        this.jsonRpc.removeExtension({extensionArtifactId:this.artifact}).then(jsonRpcResponse => {
            let outcome = jsonRpcResponse.result;
            if(!outcome){
                notifier.showErrorMessage(this.name + " " + msg('removal failed', { id: 'extensions-remove-failed' }));
            }
        });
    }

    _renderGuideDetails() {
        return this.guide
          ? html`<span style="cursor:pointer;color:var(--lumo-primary-text-color);" @click=${this._guide}>${this.guide}</span>`
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
            return html`<code style="font-family: 'JetBrains Mono', 'Fira Code', monospace; font-size: var(--lumo-font-size-s);">${this.artifact}</code>`;
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
            composed: true
          };
          this.dispatchEvent(new CustomEvent('favourite', options));
        }
    }
}

customElements.define('qwc-extension', QwcExtension);
