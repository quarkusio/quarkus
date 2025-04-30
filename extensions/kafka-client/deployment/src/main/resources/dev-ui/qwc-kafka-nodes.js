import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import { columnBodyRenderer, columnHeaderRenderer } from '@vaadin/grid/lit.js';

/**
 * This component shows the Kafka Nodes
 */
export class QwcKafkaNodes extends QwcHotReloadElement { 
    jsonRpc = new JsonRpc(this);
    
    static styles = css`
        .noGridHeader::part(header-cell){
            display: none;
        }
        .header, .nodes {
            padding-right: 30px;
            padding-left: 30px;
        }
    `;

    static properties = {
        _info: {state: true}
    };

    constructor() { 
        super();
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload(){
        this.jsonRpc.getInfo().then(jsonRpcResponse => {
            this._info = jsonRpcResponse.result;
        });
    }

    render() { 
        if(this._info){
            let header = [];
            header.push({key:"Kafka cluster id", value: this._info.clusterInfo.id});
            header.push({key:"Controller node (broker)", value: this._info.broker});
            header.push({key:"ACL operations", value: this._info.clusterInfo.aclOperations});

            return html`<div class="header">
                            <vaadin-grid .items="${header}" class="noGridHeader" theme="no-row-borders" all-rows-visible>
                                <vaadin-grid-column path="key" width="7em" ${columnBodyRenderer(this._keyRenderer, [])}></vaadin-grid-column>
                                <vaadin-grid-column path="value"></vaadin-grid-column>
                            </vaadin-grid>
                        </div>
                        <div class="nodes">
                            <h3>Cluster Nodes</h3>
                            <vaadin-grid .items="${this._info.clusterInfo.nodes}" theme="compact" all-rows-visible>
                                <vaadin-grid-column path="id" ${columnHeaderRenderer(this._idHeaderRenderer, [])}></vaadin-grid-column>    
                                <vaadin-grid-column path="host" ${columnHeaderRenderer(this._hostHeaderRenderer, [])}></vaadin-grid-column>
                                <vaadin-grid-column path="port" ${columnHeaderRenderer(this._portHeaderRenderer, [])}></vaadin-grid-column>
                            </vaadin-grid>
                        </div>
                        `;
        } else {
            return html`<vaadin-progress-bar class="progress" indeterminate></vaadin-progress-bar>`;
        }
        
    }

    _keyRenderer(info){
        return html`<b>${info.key}</b>`;
    }

    _idHeaderRenderer(){
        return html`<b>ID</b>`;
    }
    _hostHeaderRenderer(){
        return html`<b>Host</b>`;
    }
    _portHeaderRenderer(){
        return html`<b>Port</b>`;
    }
}

customElements.define('qwc-kafka-nodes', QwcKafkaNodes);