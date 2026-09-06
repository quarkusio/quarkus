import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the registered WebSocket server endpoints. It lists
 * each endpoint's class, path, inbound execution mode, declared callbacks and the
 * current number of active connections. Unlike the Dev UI, it offers no message
 * injection and no connection management - it is purely a listing.
 */
export class PwcWsnEndpoints extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            overflow: auto;
            gap: 20px;
            padding: 10px;
        }
        .toolbar {
            display: flex;
            justify-content: flex-end;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        code {
            font-size: 85%;
        }
        .annotation {
            color: var(--lumo-contrast-50pct);
        }
        .callbacks {
            margin: 0;
            padding-left: 18px;
        }
        .mode {
            display: inline-block;
            padding: 1px 8px;
            border-radius: var(--lumo-border-radius-s);
            background-color: var(--lumo-contrast-10pct);
            font-size: var(--lumo-font-size-xs);
        }
        .count {
            font-weight: 600;
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _endpoints: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getEndpoints().then(jsonRpcResponse => {
            this._endpoints = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._endpoints) {
            return html`<span class="empty">Loading endpoints...</span>`;
        }
        if (this._endpoints.length === 0) {
            return html`<span class="empty">No WebSocket server endpoints.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-grid .items="${this._endpoints}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-sort-column auto-width header="Path" path="path" frozen
                    ${columnBodyRenderer(this._pathRenderer, [])}></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Endpoint class" path="clazz"
                    ${columnBodyRenderer(this._clazzRenderer, [])}></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width flex-grow="0" header="Connections" path="connectionCount"
                    ${columnBodyRenderer(this._connectionsRenderer, [])}></vaadin-grid-sort-column>
                <vaadin-grid-column auto-width flex-grow="0" header="Execution mode"
                    ${columnBodyRenderer(this._modeRenderer, [])}></vaadin-grid-column>
                <vaadin-grid-column auto-width header="Callbacks"
                    ${columnBodyRenderer(this._callbacksRenderer, [])}></vaadin-grid-column>
            </vaadin-grid>`;
    }

    _pathRenderer(endpoint) {
        return html`<code>${endpoint.path}</code>`;
    }

    _clazzRenderer(endpoint) {
        return html`<code>${endpoint.clazz}</code>`;
    }

    _connectionsRenderer(endpoint) {
        return html`<span class="count">${endpoint.connectionCount}</span>`;
    }

    _modeRenderer(endpoint) {
        return html`<span class="mode">${endpoint.executionMode}</span>`;
    }

    _callbacksRenderer(endpoint) {
        if (!endpoint.callbacks || endpoint.callbacks.length === 0) {
            return html``;
        }
        return html`<ul class="callbacks">
            ${endpoint.callbacks.map(callback =>
                html`<li><span class="annotation"><code>${callback.annotation}</code></span>&nbsp;<code>${callback.method}</code></li>`
            )}
        </ul>`;
    }
}
customElements.define('pwc-wsn-endpoints', PwcWsnEndpoints);
