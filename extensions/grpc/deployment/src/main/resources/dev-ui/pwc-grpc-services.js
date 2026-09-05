import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the registered gRPC services. It lists each service,
 * its serving status, implementation class and the methods it defines (name +
 * streaming type). It deliberately omits the Dev UI's invoke / test client:
 * there is no way to call a method or send a message from this view.
 */
export class PwcGrpcServices extends LitElement {

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
        .method {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            margin: 2px 0;
        }
        .type {
            display: inline-block;
            padding: 1px 8px;
            border-radius: var(--lumo-border-radius-s);
            background-color: var(--lumo-contrast-10pct);
            font-size: var(--lumo-font-size-xs);
        }
        .serving {
            color: var(--lumo-success-text-color);
        }
        .not-serving {
            color: var(--lumo-error-text-color);
        }
        .unknown {
            color: var(--lumo-secondary-text-color);
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _services: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getServices().then(jsonRpcResponse => {
            this._services = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._services) {
            return html`<span class="empty">Loading gRPC services...</span>`;
        }
        if (this._services.length === 0) {
            return html`<span class="empty">No gRPC services registered.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-grid .items="${this._services}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-column auto-width flex-grow="0" header="Status"
                    ${columnBodyRenderer(this._statusRenderer, [])}></vaadin-grid-column>
                <vaadin-grid-sort-column auto-width header="Name" path="name" frozen></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Implementation class" path="serviceClass"></vaadin-grid-sort-column>
                <vaadin-grid-column auto-width header="Methods"
                    ${columnBodyRenderer(this._methodsRenderer, [])}></vaadin-grid-column>
            </vaadin-grid>`;
    }

    _statusRenderer(service) {
        if (service.status === 'SERVING') {
            return html`<vaadin-icon class="serving" icon="font-awesome-solid:check"></vaadin-icon>`;
        }
        if (service.status === 'NOT_SERVING') {
            return html`<vaadin-icon class="not-serving" icon="font-awesome-solid:circle-exclamation"></vaadin-icon>`;
        }
        return html`<vaadin-icon class="unknown" icon="font-awesome-solid:circle-question"></vaadin-icon>`;
    }

    _methodsRenderer(service) {
        return html`${service.methods.map(method => html`
            <div class="method">
                <span class="type">${method.type}</span>
                <code>${method.name}</code>
            </div>`)}`;
    }
}
customElements.define('pwc-grpc-services', PwcGrpcServices);
