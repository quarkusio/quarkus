import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { unsafeHTML } from 'lit-html/directives/unsafe-html.js';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@qomponent/qui-badge';
import { StorageController } from 'storage-controller';

/**
 * This component adds a custom link on the Extension card
 */
export class QwcExtensionLink extends QwcHotReloadElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            color: var(--lumo-contrast-80pct);
            font-size: small;
            padding: 2px 5px;
            text-decoration: none;
            gap: 5px;
        }
        .extensionLink {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            color: var(--lumo-contrast-80pct);
            font-size: small;
            padding: 2px 5px;
            cursor: pointer;
            text-decoration: none;
            gap: 5px;
        }
        .extensionLink:hover {
            filter: brightness(80%);
        }
        .icon {
            padding-right: 5px;
        }
        .iconAndName {
            display: flex;
            flex-direction: row;
            justify-content: flex-start;
            align-items: center;
        }
    `;

    static properties = {
        streamingLabelParams: {type: String},
        webcomponentTagName: {type: String},
        namespace: {type: String},
        extensionName: {type: String},
        iconName: {type: String},
        colorName: {type: String},
        tooltipContent: {type: String},
        displayName: {type: String},
        path:  {type: String},
        webcomponent: {type: String},
        embed: {type: Boolean},
        externalUrl: {type: String},
        dynamicUrlMethodName: {type: String},
        staticLabel: {type: String},
        dynamicLabel: {type: String},
        streamingLabel: {type: String},
        _effectiveLabel: {state: true},
        _effectiveExternalUrl: {state: true},
        _observer: {state: false},
    };
  
    _staticLabel = null;
    _dynamicLabel = null;
    _streamingLabel = null;
    _effectiveExternalUrl = null;

    set staticLabel(val) {
        if(!this._staticLabel || (this._staticLabel && this._staticLabel != val)){
            let oldVal = this._staticLabel;
            this._staticLabel = val;
            this.requestUpdate('staticLabel', oldVal);
            this.hotReload();
        }
    }
      
    get staticLabel() { 
        return this._staticLabel; 
    }

    set dynamicLabel(val) {
        if(!this._dynamicLabel || (this._dynamicLabel && this._dynamicLabel != val)){
            let oldVal = this._dynamicLabel;
            this._dynamicLabel = val;
            this.requestUpdate('dynamicLabel', oldVal);
            this.hotReload();
        }
    }
      
    get dynamicLabel() { 
        return this._dynamicLabel; 
    }
    
    set streamingLabel(val) {
        if(!this._streamingLabel || (this._streamingLabel && this._streamingLabel != val)){
            let oldVal = this._streamingLabel;
            this._streamingLabel = val;
            this.requestUpdate('streamingLabel', oldVal);
            this.hotReload();
        }
    }
      
    get streamingLabel() { 
        return this._streamingLabel; 
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload(){
        if(this._observer){
            this._observer.cancel();
        }
        
        if(this.dynamicUrlMethodName){
            let jrpc = new JsonRpc(this.namespace);
            jrpc[this.dynamicUrlMethodName]().then(jsonRpcResponse => {
                this._effectiveExternalUrl = jsonRpcResponse.result;
                this.requestUpdate();
            });
        }else {
            this._effectiveExternalUrl = this.externalUrl;
        }

        this._effectiveLabel = null;
        if(this.streamingLabel){
            this.jsonRpc = new JsonRpc(this);
            if(this.streamingLabelParams) {
                let streamingLabelParamsArray = this.streamingLabelParams.split(',');
                let storageController = new StorageController(this.webcomponentTagName);
                let params = {};
                for (const localParam of streamingLabelParamsArray){
                    let val = storageController.get(localParam);
                    if(!val)val="";
                    params[localParam] = val;
                }
                this._observer = this.jsonRpc[this.streamingLabel](params).onNext(jsonRpcResponse => {
                    let oldVal = this._effectiveLabel;
                    this._effectiveLabel = jsonRpcResponse.result;
                    this.requestUpdate('_effectiveLabel', oldVal);
                });
            }else {
                this._observer = this.jsonRpc[this.streamingLabel]().onNext(jsonRpcResponse => {
                    let oldVal = this._effectiveLabel;
                    this._effectiveLabel = jsonRpcResponse.result;
                    this.requestUpdate('_effectiveLabel', oldVal);
                });
            }
        }else if(this.dynamicLabel){
            this.jsonRpc = new JsonRpc(this);
            this.jsonRpc[this.dynamicLabel]().then(jsonRpcResponse => {
                let oldVal = this._effectiveLabel;
                this._effectiveLabel = jsonRpcResponse.result;
                this.requestUpdate('_effectiveLabel', oldVal);
            });
        }else if(this.staticLabel){
            let oldVal = this._effectiveLabel;
            this._effectiveLabel = this.staticLabel;
            this.requestUpdate('_effectiveLabel', oldVal);
        }else{
            let oldVal = this._effectiveLabel;
            this._effectiveLabel = null;
            this.requestUpdate('_effectiveLabel', oldVal);
        }
    }

    _getEffectiveLabel(){
        if(this.streamingLabel){
            this.jsonRpc = new JsonRpc(this);
            this._observer = this.jsonRpc[this.streamingLabel]().onNext(jsonRpcResponse => {
                this._effectiveLabel = jsonRpcResponse.result;
            });
        }else if(this.dynamicLabel){
            this.jsonRpc = new JsonRpc(this);
            this.jsonRpc[this.dynamicLabel]().then(jsonRpcResponse => {
                this._effectiveLabel = jsonRpcResponse.result;
            });
        }else if(this.staticLabel){
            this._effectiveLabel = this.staticLabel;
        }
    }

    disconnectedCallback() {
        if(this._observer){
            this._observer.cancel();
        }
        super.disconnectedCallback();
    }

    render() {
        if(!this.embed && this._effectiveExternalUrl) {
            return html`${this.renderLink(this._effectiveExternalUrl, true, "_blank")}`;
        }else if(this.path){
            return html`${this.renderLink(this.path, false, "_self")}`;
        }
    }
    
    renderLink(linkRef, routerIgnore, target){
        if(linkRef){
            return html`
                <a class="extensionLink" href="${linkRef}" ?router-ignore=${routerIgnore} target="${target}" title="${this.tooltipContent}">
                    <span class="iconAndName" style="color:${this.colorName};">
                        <vaadin-icon class="icon" icon="${this.iconName}"></vaadin-icon>
                        ${this.displayName}
                    </span>
                </a>
                ${this._renderBadge()}
                `;
        }else{
            return html`<a class="extensionLink" ?router-ignore=true>
            <span class="iconAndName">
                <vaadin-icon class="icon" icon="font-awesome-solid:spinner"></vaadin-icon>
                loading ...
            </span>
        </a>${this._renderBadge()}`;
        }
    }

    _renderBadge() {
        if (this._effectiveLabel) {
            if(this.isHTML(this._effectiveLabel)){
                return html`${unsafeHTML(this._effectiveLabel)}`;
            }else{
                return html`<qui-badge tiny pill><span>${this._effectiveLabel}</span></qui-badge>`;
            }
        }
    }

    isHTML = RegExp.prototype.test.bind(/(<([^>]+)>)/i);
}
customElements.define('qwc-extension-link', QwcExtensionLink);