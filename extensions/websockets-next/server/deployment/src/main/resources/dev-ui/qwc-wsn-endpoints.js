
import { LitElement, html, css} from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/text-field';
import { JsonRpc } from 'jsonrpc';
import { endpoints } from 'build-time-data';


export class QwcWebSocketNextEndpoints extends LitElement {
    
    jsonRpc = new JsonRpc(this);
    
    static styles = css`
       :host {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }
        .endpoints-table {
          padding-bottom: 10px;
        }
        .annotation {
          color: var(--lumo-contrast-50pct);
        }
        .connections-icon {
          font-size: small;
          color: var(--lumo-contrast-50pct); 
          cursor: pointer;
        }
        .top-bar {
            display: flex;
            align-items: baseline;
            gap: 20px;
            padding-left: 20px;
            padding-right: 20px;
        }
    
        .top-bar h4 {
            color: var(--lumo-contrast-60pct);
        }
        `;
        
        
    static properties = {
        _selectedEndpoint: {state: true},
        _endpointsAndConnections: {state: true}
    };

    constructor() {
        super();
        this._selectedEndpoint = null;
    }
    
     connectedCallback() {
        super.connectedCallback();
        const generatedEndpoints = endpoints.map(e => e.generatedClazz);
        this.jsonRpc.getConnections({"endpoints": generatedEndpoints})
            .then(jsonResponse => {
                this._endpointsAndConnections = endpoints.map(e =>  {
                    e.connections = jsonResponse.result[e.generatedClazz];
                    return e;
                });
            })
            .then(() => {
                this._conntectionStatusStream = this.jsonRpc.connectionStatus().onNext(jsonResponse => {
                    const endpoint = this._endpointsAndConnections.find(e => e.generatedClazz == jsonResponse.result.endpoint);
                    if (endpoint) {
                        if (jsonResponse.result.removed) {
                            const connectionId = jsonResponse.result.id;
                            endpoint.connections = endpoint.connections.filter(c => c.id != connectionId);
                        } else {
                            endpoint.connections = [
                                ...endpoint.connections,
                                jsonResponse.result
                            ];
                        }
                        // TODO this is inefficient but I did not find a way to update the endpoint list 
                        // when a connection is added/removed
                        // https://lit.dev/docs/components/properties/#mutating-properties
                        this._endpointsAndConnections = this._endpointsAndConnections.map(e => e);
                    }
            }
        ); 
      });
    }
    
    disconnectedCallback() {
        super.disconnectedCallback();
        this._conntectionStatusStream.cancel();
    }
    
    render() {
        if (this._endpointsAndConnections){
            if (this._selectedEndpoint){
               return this._renderConnections();
            } else{
               return this._renderEndpoints();
            }
        } else {
            return html`<span>Loading endpoints...</span>`;
        }
    }
    
     // TODO I'm not really sure this info is interesting enough 
     // <vaadin-grid-column auto-width
     //   header="Execution mode"
     //   ${columnBodyRenderer(this._renderExecutionMode, [])}
     // resizable>
     // </vaadin-grid-column>
     _renderEndpoints(){
            return html`
                <vaadin-grid .items="${this._endpointsAndConnections}" class="endpoints-table" theme="no-border" all-rows-visible>
                    <vaadin-grid-column auto-width
                        header="Endpoint"
                        ${columnBodyRenderer(this._renderClazz, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Path"
                        ${columnBodyRenderer(this._renderPath, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Callbacks"
                        ${columnBodyRenderer(this._renderCallbacks, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Connections"
                        ${columnBodyRenderer(this._renderConnectionsButton, [])}
                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>
                `;
    }
    
    _renderConnections(){
            return html`
                ${this._renderTopBar()}
                <vaadin-grid .items="${this._selectedEndpoint.connections}" class="connections-table" theme="no-border" all-rows-visible>
                    <vaadin-grid-column auto-width
                        header="Id"
                        ${columnBodyRenderer(this._renderId, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Handshake Path"
                        ${columnBodyRenderer(this._renderHandshakePath, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Creation time"
                        ${columnBodyRenderer(this._renderCreationTime, [])}
                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>
                `;
    }
    
    _renderTopBar(){
            return html`
                    <div class="top-bar">
                        <vaadin-button @click="${this._showEndpoints}">
                            <vaadin-icon icon="font-awesome-solid:caret-left" slot="prefix"></vaadin-icon>
                            Back
                        </vaadin-button>
                        <h4>${this._selectedEndpoint.clazz} · Open Connections</h4>
                    </div>`;
    }
     
    _renderPath(endpoint) {
        return html`
            <code>${endpoint.path}</code>
        `;
    }
    
    _renderClazz(endpoint) {
        return html`
            <strong>${endpoint.clazz}</strong>
        `;
    }
    
    _renderExecutionMode(endpoint) {
        return html`
            ${endpoint.executionMode}
        `;
    }
    
     _renderCallbacks(endpoint) {
         return endpoint.callbacks ? html`<ul>
            ${ endpoint.callbacks.map(callback =>
                html`<li><div class="annotation"><code>${callback.annotation}</code></div><div><code>${callback.method}</code></div></li>`
            )}</ul>`: html``;
    }
    
    _renderConnectionsButton(endpoint) {
        let ret = html`
                <vaadin-icon class="connections-icon" icon="font-awesome-solid:plug" @click=${() => this._showConnections(endpoint)}></vaadin-icon>
                `;
        ret = html`${ret} <span>${endpoint.connections.length}</span>`;
        return ret;
    }

    _renderId(connection) {
        return html`
            ${connection.id}
        `;
    }
    
    _renderHandshakePath(connection) {
        return html`
            <code>${connection.handshakePath}</code>
        `;
    }
    
    _renderCreationTime(connection) {
        return html`
            ${connection.creationTime}
        `;
    }
    
    _showConnections(endpoint){
        this._selectedEndpoint = endpoint;
    }
    
    _showEndpoints(){
        this._selectedEndpoint = null;
    }

}
customElements.define('qwc-wsn-endpoints', QwcWebSocketNextEndpoints);
