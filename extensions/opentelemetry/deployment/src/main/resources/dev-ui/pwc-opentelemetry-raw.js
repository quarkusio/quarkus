import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/grid/vaadin-grid-filter-column.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI raw view of the OpenTelemetry configuration. It is the
 * companion to the card page: the same secret-safe OTelInfo, flattened into a
 * single searchable, sortable Property/Value table - the place to scan or look up
 * an exact value. It reads only configuration and mutates nothing; the same
 * omissions apply (no OTLP headers, key/cert or trust-cert, endpoint credentials
 * stripped) because it consumes the identical getInfo() payload.
 */
export class PwcOpentelemetryRaw extends LitElement {

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
        .datatable {
            flex: 1;
            min-height: 400px;
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    // Ordered [field, label] pairs mirroring the cards, so the raw table reads top to bottom.
    static ROWS = [
        ['sdkEnabled', 'SDK enabled'],
        ['sdkDisabled', 'SDK disabled (runtime)'],
        ['tracesEnabled', 'Tracing enabled'],
        ['spanExportEnabled', 'Span export active'],
        ['serviceName', 'Service name'],
        ['simpleProcessor', 'Simple processor'],
        ['propagators', 'Propagators'],
        ['sampler', 'Sampler'],
        ['samplerArg', 'Sampler argument'],
        ['suppressNonApplicationUris', 'Suppress non-application URIs'],
        ['includeStaticResources', 'Include static resources'],
        ['exporters', 'Exporters'],
        ['exporterEndpoint', 'Exporter endpoint'],
        ['exporterProtocol', 'Exporter protocol'],
        ['exporterCompression', 'Exporter compression'],
        ['exporterTimeoutMs', 'Exporter timeout (ms)'],
        ['headersConfigured', 'Headers configured']
    ];

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
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-grid .items="${this._rows()}" class="datatable" theme="no-border row-stripes">
                <vaadin-grid-filter-column auto-width header="Property" path="property" frozen></vaadin-grid-filter-column>
                <vaadin-grid-filter-column auto-width header="Value" path="value"></vaadin-grid-filter-column>
            </vaadin-grid>`;
    }

    _rows() {
        return PwcOpentelemetryRaw.ROWS.map(([field, label]) => ({
            property: label,
            value: this._format(this._info[field])
        }));
    }

    _format(value) {
        if (value === null || value === undefined || value === '') {
            return '-';
        }
        if (Array.isArray(value)) {
            return value.length ? value.join(', ') : '-';
        }
        return String(value);
    }
}
customElements.define('pwc-opentelemetry-raw', PwcOpentelemetryRaw);
