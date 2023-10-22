import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { JsonRpc } from 'jsonrpc';

import { Graphviz } from "https://cdn.jsdelivr.net/npm/@hpcc-js/wasm/dist/graphviz.js";

import '@vaadin/details';
import '@vaadin/tabsheet';
import '@vaadin/tabs';
import 'qui-badge';
import 'qui-code-block';

/**
 * This component shows the Kafka Streams Topology
 */
export class QwcKafkaStreamsTopology extends QwcHotReloadElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        .itemHead {
            margin-left: 25px;
            width:100em;
            border-bottom: 1px solid var(--lumo-contrast-30pct)
        }
        .itemLabel {
            display: inline-block;
            width: 125px;
        }
        .itemDetails {
            margin-left: 25px;
        }
        .tabMargin {
            margin-left: 25px;
        }
        .codeBlock {
            height: 100%;
        }
    `;

    static properties = {
        _topology: {state: true},
        _graphviz: {state: true}
    };

    constructor() {
        super();
        this._topology = null;
        this._graphviz = null;
    }

    connectedCallback() {
        super.connectedCallback();
        Graphviz.load().then(r => this._graphviz = r);
        this.hotReload()
    }

    render() {
        if (this._topology) {
            return html`${this._renderDetails()}
                        ${this._renderDescription()}`;
        }

        return html`<qwc-no-data message="You do not have any Topology."
                                    link="https://quarkus.io/guides/kafka-streams"
                                linkText="Learn how to write Kafka Streams">
                    </qwc-no-data>`;
    }

    hotReload() {
        this._topology = null;
        this.jsonRpc.getTopology().then(jsonRpcResponse => {
            this._topology = jsonRpcResponse.result;
        });
    }

    _renderDetails() {
        return html`<h3>Details</h3>
                    <vaadin-details theme="reverse">
                      <vaadin-details-summary slot="summary" style="width:100em">
                        <div class="itemHead"><span class="itemLabel">Sub-topologies:</span>
                        <qui-badge>${this._topology.subTopologies.length}</qui-badge></div>
                      </vaadin-details-summary>
                      <div class="itemDetails">${this._topology.subTopologies.map((subTopology) => html`<qui-badge level="contrast" icon="font-awesome-solid:diagram-project" style="margin-right:5px">${subTopology}</qui-badge>`)}</div>
                    </vaadin-details>
                    <vaadin-details opened theme="reverse">
                      <vaadin-details-summary slot="summary" style="width:100em">
                        <div class="itemHead"><span class="itemLabel">Topic sources:</span>
                        <qui-badge>${this._topology.sources.length}</qui-badge></div>
                      </vaadin-details-summary>
                      <div class="itemDetails">${this._topology.sources.map((source) => html`<qui-badge level="contrast" icon="font-awesome-solid:right-to-bracket" style="margin-right:5px"><span>${source}</span></qui-badge>`)}</div>
                    </vaadin-details>
                    <vaadin-details opened theme="reverse">
                      <vaadin-details-summary slot="summary" style="width:100em">
                        <div class="itemHead"><span class="itemLabel">Topic sinks:</span>
                        <qui-badge>${this._topology.sinks.length}</qui-badge></div>
                      </vaadin-details-summary>
                      <div class="itemDetails">${this._topology.sinks.map((sink) => html`<qui-badge level="contrast" icon="font-awesome-solid:right-from-bracket" style="margin-right:5px">${sink}</qui-badge>`)}</div>
                    </vaadin-details>
                    <vaadin-details opened theme="reverse">
                      <vaadin-details-summary slot="summary" style="width:100em">
                        <div class="itemHead"><span class="itemLabel">State stores:</span>
                        <qui-badge>${this._topology.stores.length}</qui-badge></div>
                      </vaadin-details-summary>
                      <div class="itemDetails">${this._topology.stores.map((store) => html`<qui-badge level="contrast" icon="font-awesome-solid:database" style="margin-right:5px">${store}</qui-badge>`)}</div>
                    </vaadin-details>`;
    }

    _renderDescription() {
        return html`<h3>Description</h3>
                    <vaadin-tabsheet class="tabMargin">
                      <vaadin-tabs slot="tabs">
                        <vaadin-tab id="graphTab">Graph</vaadin-tab>
                        <vaadin-tab id="describeTab">Describe</vaadin-tab>
                        <vaadin-tab id="graphvizTab">Graphviz</vaadin-tab>
                        <vaadin-tab id="mermaidTab">Mermaid</vaadin-tab>
                      </vaadin-tabs>
                      <div tab="graphTab">${this._renderGraph()}</div>
                      <div tab="describeTab" class="codeBlock">
                        <qui-code-block mode='text' content='${this._topology.describe}'></qui-code-block>
                      </div>
                      <div tab="graphvizTab" class="codeBlock">
                        <qui-code-block mode='gv' content='${this._topology.graphviz}'></qui-code-block>
                      </div>
                      <div tab="mermaidTab" class="codeBlock">
                        <qui-code-block mode='mermaid' content='${this._topology.mermaid}' class="codeBlock"></qui-code-block>
                      </div>
                    </vaadin-tabsheet>`;
    }

    _renderGraph() {
        if (this._graphviz) {
            let g = this._graphviz.dot(this._topology.graphviz);
            return html`${unsafeHTML(g)}`;
        }

        return html`Graph engine not started.`;
    }
}
customElements.define('qwc-kafka-streams-topology', QwcKafkaStreamsTopology);