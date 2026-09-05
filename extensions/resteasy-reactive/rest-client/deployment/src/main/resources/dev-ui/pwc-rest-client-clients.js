import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the registered REST clients. It lists each client's
 * interface, config key, whether it is an injectable CDI bean and its configured
 * base URL. Any credentials embedded in the base URL are stripped server-side,
 * and no request can be invoked from this view.
 */
export class PwcRestClientClients extends LitElement {

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
        .bean {
            color: var(--lumo-success-text-color);
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
            return html`<span class="empty">Loading REST clients...</span>`;
        }
        if (this._clients.length === 0) {
            return html`<span class="empty">No REST clients.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-grid .items="${this._clients}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-sort-column auto-width header="Client interface" path="clientInterface"
                    ${columnBodyRenderer(this._interfaceRenderer, [])}></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Base URL" path="baseUrl"
                    ${columnBodyRenderer(this._baseUrlRenderer, [])}></vaadin-grid-sort-column>
                <vaadin-grid-column auto-width header="Config key"
                    ${columnBodyRenderer(this._configKeyRenderer, [])}></vaadin-grid-column>
                <vaadin-grid-column auto-width flex-grow="0" header="CDI bean"
                    ${columnBodyRenderer(this._isBeanRenderer, [])}></vaadin-grid-column>
            </vaadin-grid>`;
    }

    _interfaceRenderer(client) {
        return html`<code>${client.clientInterface}</code>`;
    }

    _baseUrlRenderer(client) {
        return client.baseUrl
            ? html`<code>${client.baseUrl}</code>`
            : html`<span class="empty">-</span>`;
    }

    _configKeyRenderer(client) {
        return client.configKey
            ? html`<code>${client.configKey}</code>`
            : html`<span class="empty">-</span>`;
    }

    _isBeanRenderer(client) {
        return client.isBean
            ? html`<vaadin-icon class="bean" icon="font-awesome-solid:check"></vaadin-icon>`
            : html``;
    }
}
customElements.define('pwc-rest-client-clients', PwcRestClientClients);
