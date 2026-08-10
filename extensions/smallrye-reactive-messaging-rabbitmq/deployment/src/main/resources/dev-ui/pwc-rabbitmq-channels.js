import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the RabbitMQ reactive-messaging channels.
 * Shows the exchange / queue / routing-key binding per channel, derived from
 * configuration - no credentials, no message browsing or production.
 */
export class PwcRabbitmqChannels extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            overflow: auto;
            gap: 15px;
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
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _channels: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getChannels().then(jsonRpcResponse => {
            this._channels = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._channels) {
            return html`<span class="empty">Loading RabbitMQ channels...</span>`;
        }
        if (this._channels.length === 0) {
            return html`<span class="empty">No RabbitMQ channels configured.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-grid .items="${this._channels}" theme="no-border row-stripes compact" all-rows-visible>
                <vaadin-grid-sort-column auto-width header="Channel" path="channel" frozen></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Direction" path="direction"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Exchange" path="exchange"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Exchange type" path="exchangeType"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Queue" path="queue"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Routing keys" path="routingKeys"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }
}
customElements.define('pwc-rabbitmq-channels', PwcRabbitmqChannels);
