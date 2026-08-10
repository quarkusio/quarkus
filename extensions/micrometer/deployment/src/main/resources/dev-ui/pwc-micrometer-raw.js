import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/grid/vaadin-grid-filter-column.js';

/**
 * Read-only Prod UI raw metrics table. This is the companion to the graphs page: it lists every
 * registered meter with its exact formatted value(s), type, tags and unit in a searchable,
 * sortable, filterable grid - the place to look up a precise number or a meter that is not
 * broken out on the graphs page. Values stream live over JSON-RPC. Reading meters is
 * non-destructive - no configuration or mutation.
 */
export class PwcMicrometerRaw extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
        }
        .toolbar {
            display: flex;
            justify-content: flex-end;
            align-items: center;
            padding: 5px 10px;
        }
        .live {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            font-size: var(--lumo-font-size-s);
            color: var(--lumo-secondary-text-color);
        }
        .live .dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: var(--lumo-success-color);
            animation: pui-pulse 2s ease-in-out infinite;
        }
        @keyframes pui-pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.3; }
        }
        .datatable {
            flex: 1;
            min-height: 400px;
            padding: 0 10px 10px;
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _meters: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        // Subscribe to the live meter stream so the table stays current without a manual refresh.
        this._subscription = this.jsonRpc.streamMeters().onNext(response => {
            this._meters = response.result;
        });
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        if (this._subscription) {
            this._subscription.cancel();
            this._subscription = null;
        }
    }

    render() {
        if (!this._meters) {
            return html`<span class="empty">Loading meters...</span>`;
        }
        if (this._meters.length === 0) {
            return html`<span class="empty">No meters registered.</span>`;
        }
        return html`
            <div class="toolbar">
                <span class="live"><span class="dot"></span> Live</span>
            </div>
            <vaadin-grid .items="${this._meters}" class="datatable" theme="no-border row-stripes">
                <vaadin-grid-filter-column auto-width header="Name" path="name" frozen></vaadin-grid-filter-column>
                <vaadin-grid-sort-column auto-width header="Type" path="type"></vaadin-grid-sort-column>
                <vaadin-grid-filter-column auto-width header="Tags" path="tags"></vaadin-grid-filter-column>
                <vaadin-grid-sort-column auto-width header="Value" path="value"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Unit" path="baseUnit"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }
}
customElements.define('pwc-micrometer-raw', PwcMicrometerRaw);
