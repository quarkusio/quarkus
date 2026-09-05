import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@qomponent/qui-card';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the Vert.x instance: event-loop and worker pool
 * sizing, execution-time guards and a few live flags. It reads only
 * configuration and the running Vertx bean - nothing is mutated - and none of
 * the values are secrets.
 */
export class PwcVertx extends LitElement {

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
        .on { color: var(--lumo-success-text-color); font-weight: 700; }
        .off { color: var(--lumo-secondary-text-color); font-weight: 700; }
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
            return html`<span class="empty">Loading Vert.x status...</span>`;
        }
        const info = this._info;
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <div class="cards">
                ${this._renderPoolsCard(info)}
                ${this._renderGuardsCard(info)}
                ${this._renderRuntimeCard(info)}
            </div>`;
    }

    _renderPoolsCard(info) {
        return html`
            <qui-card header="Pools">
                <div slot="content" class="card-content">
                    <div class="row">
                        <span class="label">Event-loop pool size</span>
                        <span class="value">${info.eventLoopPoolSize}${info.eventLoopPoolConfigured === null ? ' (default)' : ''}</span>
                    </div>
                    <div class="row">
                        <span class="label">Worker pool size</span>
                        <span class="value">${info.workerPoolSize}</span>
                    </div>
                    <div class="row">
                        <span class="label">Internal blocking pool size</span>
                        <span class="value">${info.internalBlockingPoolSize}</span>
                    </div>
                    <div class="row">
                        <span class="label">Queue size</span>
                        <span class="value">${info.queueSize === null ? 'unbounded' : info.queueSize}</span>
                    </div>
                    <div class="row">
                        <span class="label">Keep-alive time</span>
                        <span class="value">${info.keepAliveTimeSeconds} s</span>
                    </div>
                </div>
            </qui-card>`;
    }

    _renderGuardsCard(info) {
        return html`
            <qui-card header="Execution guards">
                <div slot="content" class="card-content">
                    <div class="row">
                        <span class="label">Max event-loop execute time</span>
                        <span class="value">${info.maxEventLoopExecuteTimeMs} ms</span>
                    </div>
                    <div class="row">
                        <span class="label">Max worker execute time</span>
                        <span class="value">${info.maxWorkerExecuteTimeMs} ms</span>
                    </div>
                    <div class="row">
                        <span class="label">Warning exception time</span>
                        <span class="value">${info.warningExceptionTimeMs} ms</span>
                    </div>
                    <div class="row">
                        <span class="label">Blocked thread check interval</span>
                        <span class="value">${info.blockedThreadCheckIntervalMs} ms</span>
                    </div>
                </div>
            </qui-card>`;
    }

    _renderRuntimeCard(info) {
        return html`
            <qui-card header="Runtime">
                <div slot="content" class="card-content">
                    <div class="row">
                        <span class="label">Available processors</span>
                        <span class="value">${info.availableProcessors}</span>
                    </div>
                    ${this._boolRow('Clustered', info.clustered)}
                    ${this._boolRow('Native transport', info.nativeTransportEnabled)}
                    ${this._boolRow('File caching', info.caching)}
                    ${this._boolRow('Classpath resolving', info.classpathResolving)}
                    ${this._boolRow('Pool prefill', info.prefill)}
                    ${this._boolRow('Async DNS', info.useAsyncDNS)}
                </div>
            </qui-card>`;
    }

    _boolRow(label, value) {
        return html`
            <div class="row">
                <span class="label">${label}</span>
                <span class="value ${value ? 'on' : 'off'}">${value ? 'Yes' : 'No'}</span>
            </div>`;
    }
}
customElements.define('pwc-vertx', PwcVertx);
