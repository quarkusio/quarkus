import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the registered JAX-RS (Quarkus REST) endpoints. It
 * lists each endpoint's HTTP method, path, path parameters, resource class and
 * the produced / consumed media types. It deliberately omits the Dev UI's
 * endpoint-score diagnostics and offers no request invocation.
 */
export class PwcResteasyReactiveEndpoints extends LitElement {

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
            display: inline-block;
            padding: 1px 8px;
            border-radius: var(--lumo-border-radius-s);
            background-color: var(--lumo-contrast-10pct);
            font-size: var(--lumo-font-size-xs);
            text-transform: uppercase;
        }
        .media {
            display: inline-block;
            margin-right: 4px;
            padding: 1px 6px;
            border-radius: var(--lumo-border-radius-s);
            background-color: var(--lumo-contrast-5pct);
            font-size: var(--lumo-font-size-xs);
        }
        .param {
            display: inline-block;
            margin-right: 4px;
            padding: 1px 6px;
            border-radius: var(--lumo-border-radius-s);
            background-color: var(--lumo-primary-color-10pct);
            font-size: var(--lumo-font-size-xs);
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
            return html`<span class="empty">No JAX-RS endpoints.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-grid .items="${this._endpoints}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-column auto-width flex-grow="0" header="Method"
                    ${columnBodyRenderer(this._methodRenderer, [])}></vaadin-grid-column>
                <vaadin-grid-sort-column auto-width header="Path" path="path" frozen></vaadin-grid-sort-column>
                <vaadin-grid-column auto-width header="Path params"
                    ${columnBodyRenderer(this._paramsRenderer, [])}></vaadin-grid-column>
                <vaadin-grid-column auto-width header="Produces"
                    ${columnBodyRenderer(this._producesRenderer, [])}></vaadin-grid-column>
                <vaadin-grid-column auto-width header="Consumes"
                    ${columnBodyRenderer(this._consumesRenderer, [])}></vaadin-grid-column>
                <vaadin-grid-sort-column auto-width header="Resource class" path="resourceClass"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }

    _methodRenderer(endpoint) {
        return html`<span class="method">${endpoint.httpMethod}</span>`;
    }

    _paramsRenderer(endpoint) {
        return html`${endpoint.pathParameters.map(param => html`<span class="param">${param}</span>`)}`;
    }

    _producesRenderer(endpoint) {
        return html`${endpoint.produces.map(mediaType => html`<span class="media">${mediaType}</span>`)}`;
    }

    _consumesRenderer(endpoint) {
        return html`${endpoint.consumes.map(mediaType => html`<span class="media">${mediaType}</span>`)}`;
    }
}
customElements.define('pwc-resteasy-reactive-endpoints', PwcResteasyReactiveEndpoints);
