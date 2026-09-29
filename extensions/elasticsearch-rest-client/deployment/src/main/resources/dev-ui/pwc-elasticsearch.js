import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@qomponent/qui-card';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the Elasticsearch REST client. It shows the
 * configured hosts, protocol, connection pool sizing and timeouts, plus the live
 * cluster health (status, node and shard counts) obtained from the read-only
 * GET /_cluster/health query. It issues no other request - nothing is written or
 * mutated - and never shows credentials.
 */
export class PwcElasticsearch extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            padding: 10px;
            gap: 10px;
            overflow: auto;
        }
        .toolbar {
            display: flex;
            justify-content: flex-end;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .cards {
            display: flex;
            flex-wrap: wrap;
            gap: 15px;
        }
        qui-card {
            min-width: 320px;
        }
        .card-content {
            display: flex;
            flex-direction: column;
            gap: 6px;
            padding: 12px;
        }
        .row {
            display: flex;
            justify-content: space-between;
            gap: 20px;
        }
        .label {
            color: var(--lumo-secondary-text-color);
        }
        .value {
            font-weight: 600;
            text-align: right;
        }
        .hosts {
            display: flex;
            flex-direction: column;
            align-items: flex-end;
        }
        code {
            font-size: 85%;
        }
        .status-green { color: var(--lumo-success-text-color); font-weight: 700; }
        .status-yellow { color: var(--lumo-warning-text-color, #b26a00); font-weight: 700; }
        .status-red { color: var(--lumo-error-text-color); font-weight: 700; }
        .error {
            padding: 12px;
            color: var(--lumo-error-text-color);
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _info: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this._info = undefined;
        this.jsonRpc.getInfo().then(jsonRpcResponse => {
            this._info = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._info) {
            return html`<span class="empty">Loading Elasticsearch client...</span>`;
        }
        const info = this._info;
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <div class="cards">
                ${this._renderConnectionCard(info)}
                ${this._renderHealthCard(info)}
            </div>`;
    }

    _renderConnectionCard(info) {
        return html`
            <qui-card header="Connection">
                <div slot="content" class="card-content">
                    <div class="row">
                        <span class="label">Hosts</span>
                        <span class="value hosts">
                            ${info.hosts && info.hosts.length > 0
                                ? info.hosts.map(h => html`<code>${h}</code>`)
                                : html`<code>-</code>`}
                        </span>
                    </div>
                    <div class="row">
                        <span class="label">Protocol</span>
                        <span class="value">${info.protocol}</span>
                    </div>
                    <div class="row">
                        <span class="label">Max connections</span>
                        <span class="value">${info.maxConnections}</span>
                    </div>
                    <div class="row">
                        <span class="label">Max connections / route</span>
                        <span class="value">${info.maxConnectionsPerRoute}</span>
                    </div>
                    <div class="row">
                        <span class="label">Connection timeout</span>
                        <span class="value">${info.connectionTimeoutMs} ms</span>
                    </div>
                    <div class="row">
                        <span class="label">Socket timeout</span>
                        <span class="value">${info.socketTimeoutMs} ms</span>
                    </div>
                </div>
            </qui-card>`;
    }

    _renderHealthCard(info) {
        if (info.error || !info.clusterHealth) {
            return html`
                <qui-card header="Cluster health">
                    <div slot="content" class="card-content">
                        <span class="error">
                            <vaadin-icon icon="font-awesome-solid:circle-xmark"></vaadin-icon>
                            Unavailable${info.error ? html`: ${info.error}` : html``}
                        </span>
                    </div>
                </qui-card>`;
        }
        const h = info.clusterHealth;
        const latency = info.latencyMs === null ? '' : ` (${info.latencyMs} ms)`;
        return html`
            <qui-card header="Cluster health">
                <div slot="content" class="card-content">
                    <div class="row">
                        <span class="label">Cluster</span>
                        <span class="value"><code>${h.clusterName}</code></span>
                    </div>
                    <div class="row">
                        <span class="label">Status</span>
                        <span class="value ${this._statusClass(h.status)}">${(h.status || '').toUpperCase()}${latency}</span>
                    </div>
                    <div class="row">
                        <span class="label">Nodes (data)</span>
                        <span class="value">${h.numberOfNodes} (${h.numberOfDataNodes})</span>
                    </div>
                    <div class="row">
                        <span class="label">Active primary shards</span>
                        <span class="value">${h.activePrimaryShards}</span>
                    </div>
                    <div class="row">
                        <span class="label">Active shards</span>
                        <span class="value">${h.activeShards}</span>
                    </div>
                    <div class="row">
                        <span class="label">Relocating shards</span>
                        <span class="value">${h.relocatingShards}</span>
                    </div>
                    <div class="row">
                        <span class="label">Initializing shards</span>
                        <span class="value">${h.initializingShards}</span>
                    </div>
                    <div class="row">
                        <span class="label">Unassigned shards</span>
                        <span class="value">${h.unassignedShards}</span>
                    </div>
                </div>
            </qui-card>`;
    }

    _statusClass(status) {
        switch ((status || '').toLowerCase()) {
            case 'green': return 'status-green';
            case 'yellow': return 'status-yellow';
            case 'red': return 'status-red';
            default: return '';
        }
    }
}
customElements.define('pwc-elasticsearch', PwcElasticsearch);
