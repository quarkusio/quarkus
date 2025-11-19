import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { msg, str, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the keys of a specific cache.
 */
export class QwcCacheKeys extends LitElement {
    
     static styles = css`
        .datatable {
            height: 100%;
        }
        .keys {
            padding-left: 20px;
            justify-content: space-between;
            padding-right: 20px;
            height: 100%;
        }
    
        .keys h4 {
            color: var(--lumo-contrast-60pct);
            margin-bottom: 0px;
        }
    `;
    
    static properties = {
        cacheName: {type: String},
        _keys: {state: true},
        _numberOfKeys: {state: true},
        extensionName: {type: String}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this.cacheName = null;
        this._numberOfKeys = 0;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc = new JsonRpc(this.extensionName);
        this.jsonRpc.getKeys({name: this.cacheName}).then(jsonRpcResponse => {
            this._keys = jsonRpcResponse.result;
            this._numberOfKeys = jsonRpcResponse.result.length;
        });
    }

    render() {
        const nok = this._numberOfKeys;
        const cn = this.cacheName;
        return html`
                    <div class="keys">
                        <vaadin-button @click="${this._backAction}">
                            <vaadin-icon icon="font-awesome-solid:caret-left" slot="prefix"></vaadin-icon>
                            ${msg('Back', { id: 'quarkus-cache-back' })}
                        </vaadin-button>
                        <h4>${msg(str`Found ${nok} keys in ${cn}`, { id: 'quarkus-cache-found-keys' })}</h4>
                        <vaadin-grid .items="${this._keys}" class="datatable" theme="no-border">
                            <vaadin-grid-column auto-width
                                            header=""
                                            ${columnBodyRenderer(this._keyRenderer, [])}>
                            </vaadin-grid-column>
                        </vaadin-grid>
                    </div>`;
    }
    
    _keyRenderer(cacheKey) {
        return html`<code>${cacheKey}</code>`;
    }

    _backAction(){
        const back = new CustomEvent("cache-keys-back", {
            detail: {},
            bubbles: true,
            cancelable: true,
            composed: false,
        });
        this.dispatchEvent(back);
    }

}
customElements.define('qwc-cache-keys', QwcCacheKeys);
