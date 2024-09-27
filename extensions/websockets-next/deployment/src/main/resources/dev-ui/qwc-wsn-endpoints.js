
import { LitElement, html, css} from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid';
import '@vaadin/text-field';
import '@vaadin/button';
import '@vaadin/tooltip';
import '@vaadin/message-input';
import '@vaadin/message-list';
import { notifier } from 'notifier';
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
          cursor: pointer;
        }
        .top-bar {
          align-items: baseline;
          gap: 20px;
          padding-left: 20px;
          padding-right: 20px;
        }
    
        .top-bar h4 {
          color: var(--lumo-contrast-60pct);
        }
        vaadin-message.outgoing {
          background-color: hsla(214, 61%, 25%, 0.05);
          border: 2px solid rgb(255, 255, 255);
          border-radius: 9px;
        }
        .message-list {
          gap: 20px;
          padding-left: 20px;
          padding-right: 20px;  
        }
        vaadin-message-input > vaadin-text-area > textarea {
          font-family: monospace;
        }
        `;
        
        
    static properties = {
        _selectedEndpoint: {state: true},
        _selectedConnection: {state: true},
        _endpointsAndConnections: {state: true},
        _textMessages: {state: true},
        _connectionMessagesLimit: {state: false}
    };

    constructor() {
        super();
        // If not null then show the connections of the selected endpoint
        this._selectedEndpoint = null;
        // If not null then show the detail of a Dev UI connection
        this._selectedConnection = null;
        this._textMessages = [];
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
                this._connectionMessagesLimit = jsonResponse.result.connectionMessageLimit;
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
            this._textMessagesStream =  this.jsonRpc.connectionMessages().onNext(jsonResponse => {
                if (this._selectedConnection && jsonResponse.result.key == this._selectedConnection.devuiSocketKey) {
                    this._textMessages = [
                        jsonResponse.result,
                        ...this._textMessages
                    ];
                }
            });
      });
    }
    
    disconnectedCallback() {
        super.disconnectedCallback();
        this._conntectionStatusStream.cancel();
    }
    
    render() {
        if (this._endpointsAndConnections){
            if(this._selectedConnection) {
               if (this._textMessages) {
                   return this._renderConnection();
               } else {
                   return html`<span>Loading messages...</span>`;
               }
            } else if (this._selectedEndpoint){
               return this._renderConnections();
            } else{
               return this._renderEndpoints();
            }
        } else {
            return html`<span>Loading endpoints...</span>`;
        }
    }
    
     _renderEndpoints(){
            return html`
                <vaadin-grid .items="${this._endpointsAndConnections}" class="endpoints-table" theme="no-border" all-rows-visible>
                    <vaadin-grid-column auto-width
                        header="Endpoint Class"
                        ${columnBodyRenderer(this._renderClazz, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Connections"
                        ${columnBodyRenderer(this._renderConnectionsButton, [])}
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
                </vaadin-grid>
                `;
    }
    
    _renderConnections(){
            return html`
                ${this._renderTopBar()}
                <vaadin-grid .items="${this._selectedEndpoint.connections}" class="connections-table" theme="no-border" all-rows-visible>
                    <vaadin-grid-column auto-width
                        header="Type"
                        ${columnBodyRenderer(this._renderType, [])}
                        resizable>
                    </vaadin-grid-column>
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
                        header="Creation Time"
                        ${columnBodyRenderer(this._renderCreationTime, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header="Actions"
                        ${columnBodyRenderer(this._renderDevButton, [])}
                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>
                `;
    }
    
     _renderConnection(){
            return html`
                ${this._renderTopBarConnection()}
                        <vaadin-message-input 
                            @submit="${this._sendMessage}">
                            style="font-family: monospace;"
                        </vaadin-message-input>
                        <vaadin-message-list
                            class="message-list"
                            .items="${this._textMessages}"
                        ></vaadin-message-list>
                `;
    }
    
    _renderTopBar(){
            return html`
                    <div class="top-bar">
                        <vaadin-button @click="${this._showEndpoints}">
                            <vaadin-icon icon="font-awesome-solid:caret-left" slot="prefix"></vaadin-icon>
                            Back
                        </vaadin-button>
                        <h4>Open connections for endpoint: <code>${this._selectedEndpoint.clazz}</code></h4>
                    </div>`;
    }
    
    _renderTopBarConnection(){
            return html`
                    <div class="top-bar">
                        <vaadin-button @click="${() => this._showConnections(this._selectedEndpoint)}">
                            <vaadin-icon icon="font-awesome-solid:caret-left" slot="prefix"></vaadin-icon>
                            Back
                        </vaadin-button>
                        <vaadin-button @click="${this._closeDevConnection}">
                            <vaadin-icon icon="font-awesome-solid:xmark" slot="prefix"></vaadin-icon>
                            Close connection
                        </vaadin-button>
                        <vaadin-button disabled>
                            Connection messages limit: ${this._connectionMessagesLimit}
                        </vaadin-button>
                        <vaadin-button @click="${this._clearMessages}">
                            <vaadin-icon icon="font-awesome-solid:trash" slot="prefix"></vaadin-icon>
                            Clear messages
                        </vaadin-button>
                        <h4>Connection: <code>${this._selectedConnection.id}</code></h4>
                        <h3>Endpoint: <code>${this._selectedEndpoint.clazz}</code> &nbsp;|&nbsp; Handshake path: <code>${this._selectedConnection.handshakePath}</code></h3>
                    </div>`;
    }
     
    _renderPath(endpoint) {
        const inputId = endpoint.clazz.replaceAll("\.","_").replaceAll("\$","_");
        const hasPathParam = endpoint.path.indexOf('{') != -1;
        var inputPath;
        var resetButton;
        if (hasPathParam) {
            inputPath = html`
            <vaadin-text-field
                id="${inputId}"
                value="${endpoint.path}"
                 helper-text="Replace path parameters with current values"
                style="font-family: monospace;width: 15em;"
             >
            `
            resetButton = html`
            <vaadin-button @click="${() => this._resetPathInput(inputId, endpoint.path)}" label="Reset the original path" >
               <vaadin-icon icon="font-awesome-solid:rotate-right" style="padding: 0.25em"></vaadin-icon>
               <vaadin-tooltip slot="tooltip" text="Reset the value to the original endpoint path"></vaadin-tooltip>
            </vaadin-button>
            `
        } else {
            inputPath = html`
            <vaadin-text-field
                id="${inputId}"
                value="${endpoint.path}"
                readonly
                style="font-family: monospace;width: 15em;"
             >
            `
            resetButton = html``;
        }
        return html`
            ${inputPath}
            </vaadin-text-field>
            <vaadin-button @click="${() => this._openDevConnection(inputId,endpoint)}" label="Open Dev UI connection">
                Connect
                <vaadin-tooltip slot="tooltip" text="Open new Dev UI connection"></vaadin-tooltip>
            </vaadin-button>
            ${resetButton}
        `;
    }

    _renderClazz(endpoint) {
        return html`
            <strong><code>${endpoint.clazz}</code></strong>
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
                html`<li><span class="annotation"><code>${callback.annotation}</code></span>&nbsp;<code>${callback.method}</code></li>`
            )}</ul>`: html``;
    }
    
    _renderConnectionsButton(endpoint) {
        return html`
            <vaadin-button @click=${() => this._showConnections(endpoint)}>
                <vaadin-icon icon="font-awesome-solid:plug"  style="padding: 0.25em" slot="prefix"></vaadin-icon>
                ${endpoint.connections.length}
            </vaadin-button>
         `;
    }
    
    _renderType(connection) {
        if(connection.devuiSocketKey) {
            return html`<vaadin-icon icon="font-awesome-solid:flask-vial" slot="prefix">
                <vaadin-tooltip slot="tooltip" text="Dev UI connection"></vaadin-tooltip>
            </vaadin-icon>`
        } else {
            return html`<vaadin-icon icon="font-awesome-solid:gear" slot="prefix">
                <vaadin-tooltip slot="tooltip" text="Regular connection"></vaadin-tooltip>
            </vaadin-icon>`
        }
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
    
    _renderDevButton(connection) {
        if (connection.devuiSocketKey) {
            return html`
            <vaadin-button @click=${() => this._showConnectionDetail(connection)}>
                <vaadin-icon icon="font-awesome-solid:wrench"  style="padding: 0.25em" slot="prefix"></vaadin-icon>
                Manage
            </vaadin-button>
            `;    
        } else {
            return html``;
        }
    }
    
    _showConnections(endpoint) {
        this._selectedEndpoint = endpoint;
        this._selectedConnection = null;
    }
    
    _showEndpoints() {
        this._selectedEndpoint = null;
        this._selectedConnection = null;
    }
    
    _showConnectionDetail(connection) {
        this._selectedConnection = connection;
        this._textMessages = null;
        this.jsonRpc.getMessages({"connectionKey": connection.devuiSocketKey}).then(jsonResponse => {
                this._textMessages = jsonResponse.result;
        });
    }

    _openDevConnection(inputPathId, endpoint) {
        const query = '#'+ inputPathId;
        const path = this.renderRoot?.querySelector(query).value ?? null;
        if (path) {
            this.jsonRpc.openDevConnection({"path": path, "endpointPath": endpoint.path}).then(jsonResponse => {
                if (jsonResponse.result.success) {
                    notifier.showSuccessMessage("Opened Dev UI connection");
                    this._selectedEndpoint = endpoint;
                } else {
                    notifier.showErrorMessage("Unable to open Dev UI connection", "bottom-stretch");
                }
            });
        } else {
            notifier.showErrorMessage("Unable to obtain the endpoint path", "bottom-stretch");
        }
    }
    
    _closeDevConnection() {
       this.jsonRpc.closeDevConnection({"connectionKey": this._selectedConnection.devuiSocketKey}).then(jsonResponse => {
           if (jsonResponse.result.success) {
              notifier.showSuccessMessage("Closed Dev UI connection");
           } else {
              notifier.showErrorMessage("Unable to close Dev UI connection", "bottom-stretch");
           }
           this._selectedConnection = null;
           this._showConnections(this._selectedEndpoint);
       });
    }
    
    _clearMessages() {
        if (this._selectedConnection && this._selectedConnection.devuiSocketKey) {
            this.jsonRpc.clearMessages({"connectionKey": this._selectedConnection.devuiSocketKey}).then(jsonResponse => {
               if (!jsonResponse.result.success) {
                  notifier.showErrorMessage("Unable to clear messages for Dev UI connection", "bottom-stretch");
               }
               this._textMessages = [];
           });
        }
    }
    
    _resetPathInput(endpointPathId, value) {
        const query = '#'+ endpointPathId;
        const input = this.renderRoot?.querySelector(query) ?? null;
        if (input) {
            input.value = value;
        }
    }
    
    _sendMessage(e) {
        if (this._selectedConnection && this._selectedConnection.devuiSocketKey) {
            this.jsonRpc.sendTextMessage({"connectionKey": this._selectedConnection.devuiSocketKey, "message": e.detail.value}).then(jsonResponse => {
                if (jsonResponse.result.success) {
                    notifier.showSuccessMessage("Text message sent to Dev UI connection");
                } else {
                    notifier.showErrorMessage("Unable to send text message to Dev UI connection", "bottom-stretch");
                }
            });
        }
    }
    
}
customElements.define('qwc-wsn-endpoints', QwcWebSocketNextEndpoints);
