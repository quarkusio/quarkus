import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the configured MongoDB clients. For each client
 * (blocking and reactive) it shows the name, hosts, database, application name,
 * replica set, pool sizing and a live ping result (status and latency). It
 * issues no command other than ping - nothing is written or mutated - and never
 * shows credentials.
 */
export class PwcMongodbClients extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            padding: 10px;
            gap: 10px;
        }
        .toolbar {
            display: flex;
            justify-content: flex-end;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .grid {
            height: 100%;
        }
        .up {
            color: var(--lumo-success-text-color);
            font-weight: 600;
        }
        .down {
            color: var(--lumo-error-text-color);
            font-weight: 600;
        }
        .hosts {
            display: flex;
            flex-direction: column;
        }
        code {
            font-size: 85%;
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
        this._clients = undefined;
        this.jsonRpc.getClients().then(jsonRpcResponse => {
            this._clients = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._clients) {
            return html`<span class="empty">Loading MongoDB clients...</span>`;
        }
        if (this._clients.length === 0) {
            return html`<span class="empty">No MongoDB clients configured.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-grid class="grid" .items=${this._clients} theme="row-stripes" all-rows-visible>
                <vaadin-grid-sort-column path="name" header="Name" auto-width resizable
                    ${columnBodyRenderer(c => html`<code>${c.name}</code>`, [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column path="type" header="Type" auto-width resizable></vaadin-grid-sort-column>
                <vaadin-grid-column header="Hosts" auto-width resizable
                    ${columnBodyRenderer(c => this._renderHosts(c), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column path="database" header="Database" auto-width resizable
                    ${columnBodyRenderer(c => c.database ? html`<code>${c.database}</code>` : html``, [])}>
                </vaadin-grid-column>
                <vaadin-grid-column path="replicaSetName" header="Replica set" auto-width resizable></vaadin-grid-column>
                <vaadin-grid-column header="Pool (max / min)" auto-width resizable
                    ${columnBodyRenderer(c => html`${c.maxPoolSize === null ? '-' : c.maxPoolSize} / ${c.minPoolSize === null ? '-' : c.minPoolSize}`, [])}>
                </vaadin-grid-column>
                <vaadin-grid-column header="Ping" auto-width resizable
                    ${columnBodyRenderer(c => this._renderPing(c), [])}>
                </vaadin-grid-column>
            </vaadin-grid>`;
    }

    _renderHosts(client) {
        if (client.hosts && client.hosts.length > 0) {
            return html`<div class="hosts">${client.hosts.map(h => html`<code>${h}</code>`)}</div>`;
        }
        if (client.connectionString) {
            return html`<code>${client.connectionString}</code>`;
        }
        return html``;
    }

    _renderPing(client) {
        if (client.pingStatus === 'UP') {
            const latency = client.pingLatencyMs === null ? '' : ` (${client.pingLatencyMs} ms)`;
            return html`<span class="up">
                <vaadin-icon icon="font-awesome-solid:circle-check"></vaadin-icon>
                UP${latency}
            </span>`;
        }
        return html`<span class="down" title=${client.pingError || ''}>
            <vaadin-icon icon="font-awesome-solid:circle-xmark"></vaadin-icon>
            DOWN
        </span>`;
    }
}
customElements.define('pwc-mongodb-clients', PwcMongodbClients);
