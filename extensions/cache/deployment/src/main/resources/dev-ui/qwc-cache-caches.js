import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/text-area';
import '@vaadin/form-layout';
import '@vaadin/progress-bar';
import '@vaadin/checkbox';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import './qwc-cache-keys.js';
import { msg, updateWhenLocaleChanges } from 'localization';

export class QwcCacheCaches extends LitElement {

    jsonRpc = new JsonRpc(this);

    // Component style
    static styles = css`
        .datatable {
            height: 100%;
        }
        .caches {
            height: 100%;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .clearIcon {
            color: orange;
        }
        `;

    // Component properties
    static properties = {
        "_caches": {state: true},
        _selectedCache: {state: true}
    }
    
    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._selectedCache = null;
    }

    // Components callbacks

    /**
     * Called when displayed
     */
    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getAll().then(jsonRpcResponse => {
            this._caches = new Map();
            jsonRpcResponse.result.forEach(c => {
                this._caches.set(c.name, c);
            });
        });
    }

    /**
     * Called when it needs to render the components
     * @returns {*}
     */
    render() {
        if (this._caches) {
            if(this._selectedCache){
                return this._renderCacheKeys();
            }else{
                return this._renderCacheTable();
            }
        } else {
            return html`<span>${msg('Loading caches...', { id: 'quarkus-cache-loading-caches' })}</span>`;
        }
    }

    // View / Templates

    _renderCacheTable() {
        let caches = [...this._caches.values()];
        return html`
            <div class="caches">
                <vaadin-grid .items="${caches}" class="datatable" theme="no-border">
                    <vaadin-grid-column auto-width
                                        header=${msg('Name', { id: 'quarkus-cache-name' })}
                                        ${columnBodyRenderer(this._nameRenderer, [])}>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                                        header=${msg('Size', { id: 'quarkus-cache-size' })}
                                        path="size">
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                                        header=""
                                        ${columnBodyRenderer(this._actionRenderer, [])}
                                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>
            </div>`;
    }
    
    _renderCacheKeys(){
        return html`<qwc-cache-keys extensionName="${this.jsonRpc.getExtensionName()}"
                        cacheName="${this._selectedCache.name}"
                        @cache-keys-back=${this._showCacheTable}></qwc-cache-keys>`;
    }

    _actionRenderer(cache) {
        return html`
            <vaadin-button theme="small" @click=${() => this._clear(cache.name)} class="button">
                <vaadin-icon class="clearIcon" icon="font-awesome-solid:broom"></vaadin-icon> ${msg('Clear', { id: 'quarkus-cache-clear' })}
            </vaadin-button>
            &nbsp;|&nbsp;
            <vaadin-button theme="small" @click=${() => this._showCacheKeys(cache)} class="button">
                <vaadin-icon class="keysIcon" icon="font-awesome-solid:key"></vaadin-icon> ${msg('Keys', { id: 'quarkus-cache-keys-button' })}
            </vaadin-button>`;
    }

    _nameRenderer(cache) {
        return html`
            <vaadin-button theme="small" @click=${() => this._refresh(cache.name)} class="button">
                <vaadin-icon icon="font-awesome-solid:rotate"></vaadin-icon>
            </vaadin-button>
            ${cache.name}`;
    }

    _clear(name) {
        this.jsonRpc.clear({name: name}).then(jsonRpcResponse => {
            this._updateCache(jsonRpcResponse.result)
        });
    }

    _refresh(name) {
        this.jsonRpc.refresh({name: name}).then(jsonRpcResponse => {
            this._updateCache(jsonRpcResponse.result)
        });
    }

    _updateCache(cache){
        if (this._caches.has(cache.name)  && cache.size !== -1) {
            this._caches.set(cache.name, cache);
            this.requestUpdate();
        }
    }
    
    _showCacheKeys(cache){
        this._selectedCache = cache;
    }
    
    _showCacheTable(){
        this._selectedCache = null;
    }

}
customElements.define('qwc-cache-caches', QwcCacheCaches);
