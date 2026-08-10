import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the Kafka cluster.
 * Shows brokers, topics and consumer-group lag only - no topic create/delete,
 * no message browsing or production, and no secrets.
 */
export class PwcKafka extends LitElement {

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
        .section h3 {
            margin: 0 0 5px 0;
        }
        .cluster {
            display: flex;
            gap: 30px;
            flex-wrap: wrap;
            color: var(--lumo-secondary-text-color);
        }
        .cluster b {
            color: var(--lumo-body-text-color);
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
        _overview: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getOverview().then(jsonRpcResponse => {
            this._overview = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._overview) {
            return html`<span class="empty">Loading Kafka cluster...</span>`;
        }
        const o = this._overview;
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <div class="section">
                <div class="cluster">
                    <span>Cluster id: <b>${o.clusterId}</b></span>
                    <span>Controller: <b>${o.controller}</b></span>
                </div>
            </div>
            <div class="section">
                <h3>Brokers</h3>
                <vaadin-grid .items="${o.nodes}" theme="no-border row-stripes compact" all-rows-visible>
                    <vaadin-grid-sort-column auto-width header="ID" path="id"></vaadin-grid-sort-column>
                    <vaadin-grid-sort-column auto-width header="Host" path="host"></vaadin-grid-sort-column>
                    <vaadin-grid-sort-column auto-width header="Port" path="port"></vaadin-grid-sort-column>
                </vaadin-grid>
            </div>
            <div class="section">
                <h3>Topics</h3>
                ${o.topics.length === 0
                    ? html`<span class="empty">No topics.</span>`
                    : html`<vaadin-grid .items="${o.topics}" theme="no-border row-stripes compact" all-rows-visible>
                        <vaadin-grid-sort-column auto-width header="Name" path="name" frozen></vaadin-grid-sort-column>
                        <vaadin-grid-sort-column auto-width header="ID" path="id"></vaadin-grid-sort-column>
                        <vaadin-grid-sort-column auto-width header="Partitions" path="partitions"></vaadin-grid-sort-column>
                        <vaadin-grid-sort-column auto-width header="Internal" path="internal"></vaadin-grid-sort-column>
                    </vaadin-grid>`}
            </div>
            <div class="section">
                <h3>Consumer groups</h3>
                ${o.groups.length === 0
                    ? html`<span class="empty">No consumer groups.</span>`
                    : html`<vaadin-grid .items="${o.groups}" theme="no-border row-stripes compact" all-rows-visible>
                        <vaadin-grid-sort-column auto-width header="Group" path="groupId" frozen></vaadin-grid-sort-column>
                        <vaadin-grid-sort-column auto-width header="State" path="state"></vaadin-grid-sort-column>
                        <vaadin-grid-sort-column auto-width header="Coordinator" path="coordinator"></vaadin-grid-sort-column>
                        <vaadin-grid-sort-column auto-width header="Protocol" path="protocol"></vaadin-grid-sort-column>
                        <vaadin-grid-sort-column auto-width header="Members" path="members"></vaadin-grid-sort-column>
                        <vaadin-grid-sort-column auto-width header="Lag (sum)" path="lag"></vaadin-grid-sort-column>
                    </vaadin-grid>`}
            </div>`;
    }
}
customElements.define('pwc-kafka', PwcKafka);
