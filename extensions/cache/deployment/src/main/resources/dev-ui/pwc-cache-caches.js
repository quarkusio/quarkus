import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { isProdUI } from 'ui-context';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/button';
import '@vaadin/icon';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';

export class PwcCacheCaches extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: block;
            height: 100%;
        }
        .datatable {
            height: 100%;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
    `;

    static properties = {
        _caches: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getAll().then(jsonRpcResponse => {
            this._caches = jsonRpcResponse;
        });
    }

    render() {
        if (!this._caches) {
            return html`<span>Loading caches...</span>`;
        }
        return html`
            <vaadin-grid .items="${this._caches}" class="datatable" theme="no-border">
                <vaadin-grid-sort-column auto-width header="Name" path="name"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Size" path="size"></vaadin-grid-sort-column>
                <vaadin-grid-column auto-width header=""
                    ${columnBodyRenderer(this._actionRenderer, [])}>
                </vaadin-grid-column>
            </vaadin-grid>`;
    }

    _actionRenderer(cache) {
        return html`
            <vaadin-button theme="small" @click=${() => this._refresh(cache.name)} class="button">
                <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
            </vaadin-button>`;
    }

    _refresh(name) {
        this.jsonRpc.refresh({ name: name }).then(jsonRpcResponse => {
            if (this._caches) {
                this._caches = this._caches.map(c =>
                    c.name === jsonRpcResponse.name ? jsonRpcResponse : c
                );
            }
        });
    }
}
customElements.define('pwc-cache-caches', PwcCacheCaches);
