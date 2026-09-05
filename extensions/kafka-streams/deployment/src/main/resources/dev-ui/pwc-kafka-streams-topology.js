import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/tabs';
import '@vaadin/icon';
import '@vaadin/button';
import 'pui-echart-graph';

/**
 * Read-only Prod UI view of the Kafka Streams topology.
 * Renders the topology as an interactive node-link graph (ECharts, already in the
 * Prod UI bundle) using the structured nodes/edges the service now exposes, and
 * also shows the sub-topology / source / sink / store structure plus the raw
 * describe, Graphviz (DOT) and Mermaid renderings - no mutations, no secrets.
 * Unlike the Dev UI it does not pull in the dev-only wasm graph engine; the
 * Graphviz/Mermaid source is still shown as text that can be pasted into any renderer.
 */
export class PwcKafkaStreamsTopology extends LitElement {

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
        .counts {
            display: flex;
            gap: 30px;
            flex-wrap: wrap;
            color: var(--lumo-secondary-text-color);
        }
        .counts b {
            color: var(--lumo-body-text-color);
        }
        .badges {
            display: flex;
            flex-wrap: wrap;
            gap: 5px;
        }
        .badge {
            background-color: var(--lumo-contrast-10pct);
            border-radius: var(--lumo-border-radius-s);
            padding: 2px 8px;
            font-size: var(--lumo-font-size-s);
        }
        pre {
            background-color: var(--lumo-contrast-5pct);
            border-radius: var(--lumo-border-radius-s);
            padding: 10px;
            overflow: auto;
            white-space: pre;
            margin: 0;
        }
        h3 {
            margin: 0 0 5px 0;
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
        .graph {
            flex: 1;
            min-height: 500px;
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: var(--lumo-border-radius-s);
        }
    `;

    static properties = {
        _topology: { state: true },
        _selectedTab: { state: true }
    };

    constructor() {
        super();
        this._topology = null;
        this._selectedTab = 0;
    }

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getTopology().then(jsonRpcResponse => {
            this._topology = jsonRpcResponse.result;
        });
    }

    render() {
        if (!this._topology) {
            return html`<span class="empty">Loading topology...</span>`;
        }
        if (!this._topology.describe) {
            return html`<span class="empty">You do not have any Topology.</span>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-tabs @selected-changed="${(e) => this._selectedTab = e.detail.value}">
                <vaadin-tab>Graph</vaadin-tab>
                <vaadin-tab>Details</vaadin-tab>
                <vaadin-tab>Describe</vaadin-tab>
                <vaadin-tab>Graphviz</vaadin-tab>
                <vaadin-tab>Mermaid</vaadin-tab>
            </vaadin-tabs>
            ${this._renderTab()}`;
    }

    _renderTab() {
        switch (this._selectedTab) {
            case 1: return this._renderDetails();
            case 2: return html`<pre>${this._topology.describe}</pre>`;
            case 3: return html`<pre>${this._topology.graphviz}</pre>`;
            case 4: return html`<pre>${this._topology.mermaid}</pre>`;
            default: return this._renderGraph();
        }
    }

    _renderGraph() {
        const nodes = this._topology.nodes || [];
        if (nodes.length === 0) {
            return html`<span class="empty">No graph data available.</span>`;
        }
        return html`<pui-echart-graph
            class="graph"
            nodes="${JSON.stringify(nodes)}"
            edges="${JSON.stringify(this._topology.edges || [])}"></pui-echart-graph>`;
    }

    _renderDetails() {
        return html`
            <div class="counts">
                <span>Sub-topologies: <b>${this._topology.subTopologies.length}</b></span>
                <span>Sources: <b>${this._topology.sources.length}</b></span>
                <span>Sinks: <b>${this._topology.sinks.length}</b></span>
                <span>Stores: <b>${this._topology.stores.length}</b></span>
            </div>
            ${this._renderSection('Sub-topologies', this._topology.subTopologies)}
            ${this._renderSection('Sources', this._topology.sources)}
            ${this._renderSection('Sinks', this._topology.sinks)}
            ${this._renderSection('Stores', this._topology.stores)}`;
    }

    _renderSection(title, items) {
        return html`
            <div>
                <h3>${title}</h3>
                ${items.length === 0
                    ? html`<span class="empty">None.</span>`
                    : html`<div class="badges">${items.map(i => html`<span class="badge">${i}</span>`)}</div>`}
            </div>`;
    }
}
customElements.define('pwc-kafka-streams-topology', PwcKafkaStreamsTopology);
