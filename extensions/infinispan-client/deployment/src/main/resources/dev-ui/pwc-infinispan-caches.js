import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the configured Infinispan clients. For each client
 * it shows the cluster members (server addresses) and a grid of caches with
 * their server-side hit/miss statistics. No cache mutation (put/remove/clear)
 * and no credentials are exposed.
 */
export class PwcInfinispanCaches extends LitElement {

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
        .client {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        .client-header {
            display: flex;
            align-items: baseline;
            gap: 15px;
            flex-wrap: wrap;
        }
        .client-name {
            font-size: var(--lumo-font-size-l);
            font-weight: bold;
        }
        .meta {
            color: var(--lumo-secondary-text-color);
            font-size: var(--lumo-font-size-s);
        }
        .servers {
            font-family: monospace;
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _clients: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getClients().then(jsonRpcResponse => {
            this._clients = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._clients) {
            return html`<span class="empty">Loading Infinispan clients...</span>`;
        }
        if (this._clients.length === 0) {
            return html`<span class="empty">No Infinispan clients configured.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            ${this._clients.map(client => this._renderClient(client))}`;
    }

    _renderClient(client) {
        return html`
            <div class="client">
                <div class="client-header">
                    <span class="client-name">${client.name}</span>
                    <span class="meta">${client.started ? 'Started' : 'Stopped'}</span>
                    <span class="meta">Protocol: ${client.protocolVersion}</span>
                    <span class="meta">Active connections: ${client.activeConnections}</span>
                    <span class="meta">Statistics: ${client.statisticsEnabled ? 'enabled' : 'disabled'}</span>
                </div>
                <div class="meta servers">Cluster members: ${client.servers.join(', ') || '-'}</div>
                ${this._renderCaches(client.caches)}
            </div>`;
    }

    _renderCaches(caches) {
        if (!caches || caches.length === 0) {
            return html`<span class="empty">No caches.</span>`;
        }
        return html`
            <vaadin-grid .items="${caches}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-sort-column auto-width header="Cache" path="name" frozen></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Entries" path="entries"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Hits" path="hits"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Misses" path="misses"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Stores" path="stores"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Retrievals" path="retrievals"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Remove hits" path="removeHits"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Remove misses" path="removeMisses"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }
}
customElements.define('pwc-infinispan-caches', PwcInfinispanCaches);
