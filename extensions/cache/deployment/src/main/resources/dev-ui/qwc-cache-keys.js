import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';

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
    
    jsonRpc = new JsonRpc("io.quarkus.quarkus-cache");

    static properties = {
        cacheName: {type: String},
        _keys: {state: true},
        _numberOfKeys: {state: true}
    };

    constructor() {
        super();
        this.cacheName = null;
        this._numberOfKeys = 0;
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getKeys({name: this.cacheName}).then(jsonRpcResponse => {
            this._keys = jsonRpcResponse.result;
            this._numberOfKeys = jsonRpcResponse.result.length;
        });
    }

    render() {
        return html`
                    <div class="keys">
                        <vaadin-button @click="${this._backAction}">
                            <vaadin-icon icon="font-awesome-solid:caret-left" slot="prefix"></vaadin-icon>
                            Back
                        </vaadin-button>
                        <h4>Found ${this._numberOfKeys} keys in ${this.cacheName}</h4>
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