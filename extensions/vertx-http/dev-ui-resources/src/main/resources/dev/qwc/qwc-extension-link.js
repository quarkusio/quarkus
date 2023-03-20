import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import 'qui-badge';

/**
 * This component adds a custom link on the Extension card
 */
export class QwcExtensionLink extends LitElement {
  
    static styles = css`
        .extensionLink {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            color: var(--lumo-contrast);
            font-size: small;
            padding: 2px 5px;
            cursor: pointer;
            text-decoration: none;
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
            color: var(--lumo-contrast);
        }
    `;

    static properties = {
        extensionName: {type: String},
        iconName: {type: String},
        displayName: {type: String},
        staticLabel: {type: String},
        dynamicLabel: {type: String},
        streamingLabel: {type: String},
        path:  {type: String},
        webcomponent: {type: String},
        _effectiveLabel: {state: true},
        _observer: {state: false},
    };

    connectedCallback() {
        super.connectedCallback();
        if(this.streamingLabel){
            this.jsonRpc = new JsonRpc(this.extensionName);
            this._observer = this.jsonRpc[this.streamingLabel]().onNext(jsonRpcResponse => {
                this._effectiveLabel = jsonRpcResponse.result;
            });
        }else if(this.dynamicLabel){
            this.jsonRpc = new JsonRpc(this.extensionName);
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
        super.disconnectedCallback()
    }

    render() {
        let routerIgnore = false;
        if(this.webcomponent === ""){
            routerIgnore = true;
        }
        return html`
        <a class="extensionLink" href="${this.path}" ?router-ignore=${routerIgnore}>
            <span class="iconAndName">
                <vaadin-icon class="icon" icon="${this.iconName}"></vaadin-icon>
                ${this.displayName} 
            </span>
            ${this._renderBadge()} 
        </a>
        `;
    }
    
    _renderBadge() {
        if (this._effectiveLabel) {
            return html`<qui-badge tiny pill><span>${this._effectiveLabel}</span></qui-badge>`;
        }
    }
}
customElements.define('qwc-extension-link', QwcExtensionLink);