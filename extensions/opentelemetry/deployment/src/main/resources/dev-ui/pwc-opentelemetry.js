import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@qomponent/qui-card';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the OpenTelemetry configuration: SDK/tracing status,
 * span exporter(s), sampler, and the OTLP exporter endpoint/protocol/compression/
 * timeout. It reads only configuration - nothing is mutated - and never shows the
 * OTLP headers, key/cert or trust-cert (only whether headers are configured).
 */
export class PwcOpentelemetry extends LitElement {

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
            min-width: 340px;
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
            word-break: break-all;
        }
        .on { color: var(--lumo-success-text-color); font-weight: 700; }
        .off { color: var(--lumo-error-text-color); font-weight: 700; }
        code {
            font-size: 85%;
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
            return html`<span class="empty">Loading OpenTelemetry configuration...</span>`;
        }
        const info = this._info;
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <div class="cards">
                ${this._renderStatusCard(info)}
                ${this._renderSamplerCard(info)}
                ${this._renderExporterCard(info)}
            </div>`;
    }

    _renderStatusCard(info) {
        return html`
            <qui-card header="Status">
                <div slot="content" class="card-content">
                    ${this._boolRow('SDK enabled', info.sdkEnabled)}
                    ${this._boolRow('SDK disabled (runtime)', info.sdkDisabled)}
                    ${this._boolRow('Tracing enabled', info.tracesEnabled)}
                    ${this._boolRow('Span export active', info.spanExportEnabled)}
                    <div class="row">
                        <span class="label">Service name</span>
                        <span class="value">${info.serviceName || '-'}</span>
                    </div>
                    <div class="row">
                        <span class="label">Processor</span>
                        <span class="value">${info.simpleProcessor ? 'simple' : 'batch'}</span>
                    </div>
                    <div class="row">
                        <span class="label">Propagators</span>
                        <span class="value">${(info.propagators || []).join(', ') || '-'}</span>
                    </div>
                </div>
            </qui-card>`;
    }

    _renderSamplerCard(info) {
        return html`
            <qui-card header="Sampler">
                <div slot="content" class="card-content">
                    <div class="row">
                        <span class="label">Sampler</span>
                        <span class="value"><code>${info.sampler}</code></span>
                    </div>
                    <div class="row">
                        <span class="label">Sampler argument</span>
                        <span class="value">${info.samplerArg || '-'}</span>
                    </div>
                    ${this._boolRow('Suppress non-application URIs', info.suppressNonApplicationUris)}
                    ${this._boolRow('Include static resources', info.includeStaticResources)}
                </div>
            </qui-card>`;
    }

    _renderExporterCard(info) {
        return html`
            <qui-card header="OTLP exporter (traces)">
                <div slot="content" class="card-content">
                    <div class="row">
                        <span class="label">Exporters</span>
                        <span class="value">${(info.exporters || []).join(', ') || '-'}</span>
                    </div>
                    <div class="row">
                        <span class="label">Endpoint</span>
                        <span class="value">${info.exporterEndpoint ? html`<code>${info.exporterEndpoint}</code>` : '-'}</span>
                    </div>
                    <div class="row">
                        <span class="label">Protocol</span>
                        <span class="value">${info.exporterProtocol || '-'}</span>
                    </div>
                    <div class="row">
                        <span class="label">Compression</span>
                        <span class="value">${info.exporterCompression || 'none'}</span>
                    </div>
                    <div class="row">
                        <span class="label">Timeout</span>
                        <span class="value">${info.exporterTimeoutMs} ms</span>
                    </div>
                    ${this._boolRow('Headers configured', info.headersConfigured)}
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
customElements.define('pwc-opentelemetry', PwcOpentelemetry);
