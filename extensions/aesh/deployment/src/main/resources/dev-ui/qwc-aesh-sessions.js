import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/button';
import '@vaadin/icon';

/**
 * Dev UI page showing transport session info and live event log.
 */
export class QwcAeshSessions extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            gap: 16px;
            height: 100%;
            padding: 10px;
        }
        .transport-cards {
            display: flex;
            gap: 16px;
            flex-wrap: wrap;
        }
        .transport-card {
            border: 1px solid var(--lumo-contrast-20pct);
            border-radius: 8px;
            padding: 16px;
            min-width: 200px;
            background: var(--lumo-base-color);
        }
        .transport-name {
            font-size: 16px;
            font-weight: bold;
            text-transform: capitalize;
            margin-bottom: 8px;
        }
        .transport-stat {
            display: flex;
            justify-content: space-between;
            padding: 4px 0;
            font-size: 14px;
        }
        .stat-label {
            color: var(--lumo-contrast-60pct);
        }
        .stat-value {
            font-weight: 600;
        }
        .status-running {
            color: var(--lumo-success-text-color);
        }
        .status-stopped {
            color: var(--lumo-error-text-color);
        }
        .event-log-header {
            font-size: 16px;
            font-weight: bold;
        }
        .event-log {
            flex: 1;
            min-height: 0;
        }
        .event-opened {
            color: var(--lumo-success-text-color);
        }
        .event-closed {
            color: var(--lumo-contrast-60pct);
        }
    `;

    static properties = {
        _transports: { state: true },
        _eventLog: { state: true }
    };

    constructor() {
        super();
        this._transports = [];
        this._eventLog = [];
    }

    connectedCallback() {
        super.connectedCallback();
        this._loadSessionInfo();
        this._eventStream = this.jsonRpc.streamSessionEvents().onNext(jsonResponse => {
            this._eventLog = [...this._eventLog, jsonResponse.result];
            // Refresh transport counts on each event
            this._loadSessionInfo();
        });
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        if (this._eventStream) {
            this._eventStream.cancel();
        }
    }

    _loadSessionInfo() {
        this.jsonRpc.getSessionInfo().then(jsonResponse => {
            this._transports = jsonResponse.result.transports || [];
            if (!this._eventLog.length && jsonResponse.result.eventLog) {
                this._eventLog = jsonResponse.result.eventLog;
            }
        });
    }

    render() {
        return html`
            ${this._renderTransportCards()}
            <span class="event-log-header">Session Event Log</span>
            ${this._renderEventLog()}
        `;
    }

    _renderTransportCards() {
        if (!this._transports.length) {
            return html`<span>No remote transports available.</span>`;
        }
        return html`
            <div class="transport-cards">
                ${this._transports.map(t => this._renderTransportCard(t))}
            </div>
        `;
    }

    _renderTransportCard(transport) {
        const statusClass = transport.running ? 'status-running' : 'status-stopped';
        const statusText = transport.running ? 'Running' : 'Stopped';
        const maxText = transport.max === -1 ? 'unlimited' : transport.max;

        return html`
            <div class="transport-card">
                <div class="transport-name">${transport.name}</div>
                <div class="transport-stat">
                    <span class="stat-label">Status</span>
                    <span class="stat-value ${statusClass}">${statusText}</span>
                </div>
                <div class="transport-stat">
                    <span class="stat-label">Active Sessions</span>
                    <span class="stat-value">${transport.active}</span>
                </div>
                <div class="transport-stat">
                    <span class="stat-label">Max Sessions</span>
                    <span class="stat-value">${maxText}</span>
                </div>
            </div>
        `;
    }

    _renderEventLog() {
        if (!this._eventLog.length) {
            return html`<span style="color: var(--lumo-contrast-50pct)">No session events yet.</span>`;
        }
        return html`
            <vaadin-grid .items="${this._eventLog}" class="event-log" theme="no-border compact">
                <vaadin-grid-column
                    auto-width
                    header="Time"
                    path="timestamp"
                    resizable>
                </vaadin-grid-column>
                <vaadin-grid-column
                    auto-width
                    header="Event"
                    ${this._eventTypeRenderer()}
                    resizable>
                </vaadin-grid-column>
                <vaadin-grid-column
                    auto-width
                    header="Transport"
                    path="transport"
                    resizable>
                </vaadin-grid-column>
                <vaadin-grid-column
                    auto-width
                    header="Session ID"
                    path="sessionId"
                    resizable>
                </vaadin-grid-column>
            </vaadin-grid>
        `;
    }

    _eventTypeRenderer() {
        return (root, column, rowData) => {
            const eventType = rowData.item.eventType;
            const cls = eventType === 'opened' ? 'event-opened' : 'event-closed';
            root.innerHTML = `<span class="${cls}">${eventType}</span>`;
        };
    }
}

customElements.define('qwc-aesh-sessions', QwcAeshSessions);
