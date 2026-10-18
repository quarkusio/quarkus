import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { JsonRpc } from 'jsonrpc';
import { devuiState } from 'devui-state';
import { notifier } from 'notifier';
import { Graphviz } from "@hpcc-js/wasm/graphviz.js";

import '@vaadin/details';
import '@vaadin/tabs';
import '@vaadin/vertical-layout';
import 'qui-badge';
import { observeState } from 'lit-element-state';
import { themeState } from 'theme-state';
import '@quarkus-webcomponents/codeblock';
import { msg, str, updateWhenLocaleChanges } from 'localization';

/**
 * This component shows the Kafka Streams Topology
 */
export class QwcKafkaStreamsTopology extends observeState(QwcHotReloadElement) {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        .codeBlock {
            width: 100%;
            height: auto;
        }
    `;

    static properties = {
        _topology: {state: true},
        _graphviz: {state: true},
        _tabContent: {state: true}
    };

    constructor() {
        super();
        updateWhenLocaleChanges(this);
        this._topology = null;
        this._graphviz = null;
        this._tabContent = '';
    }

    connectedCallback() {
        super.connectedCallback();
        Graphviz.load().then(r => this._graphviz = r);
        this.hotReload();
    }

    render() {
        if (this._topology) {
            return html`<vaadin-tabs @selected-changed="${(e) => this._tabSelectedChanged(e.detail.value)}">
                          <vaadin-tab id="graphTab">${msg('Graph', { id: 'quarkus-kafka-streams-graph' })}</vaadin-tab>
                          <vaadin-tab id="detailsTab">${msg('Details', { id: 'quarkus-kafka-streams-details' })}</vaadin-tab>
                          <vaadin-tab id="describeTab">${msg('Describe', { id: 'quarkus-kafka-streams-describe' })}</vaadin-tab>
                          <vaadin-tab id="graphvizTab">${msg('Graphviz', { id: 'quarkus-kafka-streams-graphviz' })}</vaadin-tab>
                          <vaadin-tab id="mermaidTab">${msg('Mermaid', { id: 'quarkus-kafka-streams-mermaid' })}</vaadin-tab>
                        </vaadin-tabs>
                        <vaadin-vertical-layout theme="padding"><p id="svgSpan">${this._tabContent}</p></vaadin-vertical-layout>`;
        }

        return html`<qwc-no-data message=${msg('You do not have any Topology.', { id: 'quarkus-kafka-streams-no-topology' })}
                                    link="https://quarkus.io/guides/kafka-streams"
                                linkText=${msg('Learn how to write Kafka Streams', { id: 'quarkus-kafka-streams-learn-link' })}>
                    </qwc-no-data>`;
    }

    hotReload() {
        this._topology = null;
        this.jsonRpc.getTopology().then(jsonRpcResponse => {
            this._topology = jsonRpcResponse.result;
        });
    }

    _tabSelectedChanged(n) {
      switch(n) {
        case 1 : this._selectDetailsTab(); break;
        case 2 : this._selectDescribeTab(); break;
        case 3 : this._selectGraphvizTab(); break;
        case 4 : this._selectMermaidTab(); break;
        default : this._selectGraphTab();
      }
    }

    _selectGraphTab() {
      if (this._graphviz) {
        let g = this._graphviz.dot(this._topology.graphviz);
        this._tabContent = html`${unsafeHTML(g)}
                                <qui-badge level="contrast" icon="font-awesome-solid:download" clickable @click=${() => this._downloadTopologyAsPng()}>
                                  <span>${msg('Download as PNG', { id: 'quarkus-kafka-streams-download-png' })}</span>
                                </qui-badge>`;
      } else {
        this._tabContent = html`${msg('Graph engine not started.', { id: 'quarkus-kafka-streams-graph-engine-not-started' })}`;
      }
    }

    _downloadTopologyAsPng() {
     	let svgData = this.renderRoot?.querySelector('#svgSpan').getElementsByTagName("svg")[0];
     	let img = new Image(svgData.width.baseVal.value, svgData.height.baseVal.value);
     	img.src = `data:image/svg+xml;base64,${btoa(new XMLSerializer().serializeToString(svgData))}`;
     	img.onload = function () {
     		let cnv = document.createElement('canvas');
     		cnv.width = img.width;
     		cnv.height = img.height;
     		cnv.getContext("2d").drawImage(img, 0, 0);
     		cnv.toBlob((blob) => {
     			let lnk = document.createElement('a');
                        const dnd = lnk.download;
     			lnk.href = URL.createObjectURL(blob);
     			lnk.download = "Topology-" + devuiState.applicationInfo.applicationName + "-" + new Date().toISOString().replace(/\D/g,'') + ".png";
     			lnk.click();
     			notifier.showSuccessMessage(msg(str`${dnd} downloaded.`, { id: 'quarkus-kafka-streams-downloaded' }), 'bottom-end');
     		});
     	};
     }

    _selectDetailsTab() {
      this._tabContent = html`<table>
                                <tr>
                                  <td>${msg('Sub-topologies', { id: 'quarkus-kafka-streams-sub-topologies' })}</td><td><qui-badge>${this._topology.subTopologies.length}</qui-badge></td>
                                  <td>${this._topology.subTopologies.map((subTopology) => html`<qui-badge level="contrast" icon="font-awesome-solid:diagram-project" style="margin-right:5px">${subTopology}</qui-badge>`)}</td>
                                </tr>
                                <tr>
                                  <td>${msg('Sources', { id: 'quarkus-kafka-streams-sources' })}</td><td><qui-badge>${this._topology.sources.length}</qui-badge></td>
                                  <td>${this._topology.sources.map((source) => html`<qui-badge level="contrast" icon="font-awesome-solid:right-to-bracket" style="margin-right:5px">${source}</qui-badge>`)}</td>
                                </tr>
                                <tr>
                                  <td>${msg('Sinks', { id: 'quarkus-kafka-streams-sinks' })}</td><td><qui-badge>${this._topology.sinks.length}</qui-badge></td>
                                  <td>${this._topology.sinks.map((sink) => html`<qui-badge level="contrast" icon="font-awesome-solid:right-from-bracket" style="margin-right:5px">${sink}</qui-badge>`)}</td>
                                </tr>
                                <tr>
                                  <td>${msg('Stores', { id: 'quarkus-kafka-streams-stores' })}</td><td><qui-badge>${this._topology.stores.length}</qui-badge></td>
                                  <td>${this._topology.stores.map((store) => html`<qui-badge level="contrast" icon="font-awesome-solid:database" style="margin-right:5px">${store}</qui-badge>`)}</td>
                                </tr>
                              </table>`;
    }

    _selectDescribeTab() {
      this._tabContent = html`<qui-code-block theme='${themeState.theme.name}' content='${this._topology.describe}' class="codeBlock"></qui-code-block>`;
    }

    _selectGraphvizTab() {
      this._tabContent = html`<qui-code-block mode='gv' theme="${themeState.theme.name}" content='${this._topology.graphviz}' class="codeBlock"></qui-code-block>`;
    }

    _selectMermaidTab() {
      this._tabContent = html`<qui-code-block mode='mermaid' theme="${themeState.theme.name}" content='${this._topology.mermaid}' class="codeBlock"></qui-code-block>`;
    }
}
customElements.define('qwc-kafka-streams-topology', QwcKafkaStreamsTopology);