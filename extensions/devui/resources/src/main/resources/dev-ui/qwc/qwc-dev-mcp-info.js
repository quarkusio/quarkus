import { QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import 'qwc-no-data';
import { basepath } from 'devui-data';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';

/**
 * This component show details on the MCP Server
 */
export class QwcDevMCPInfo extends QwcHotReloadElement {
    jsonRpc = new JsonRpc("devmcp");
    
    static styles = css`
        .serverDetails {
            display: flex;
            gap: 20px;
            padding-top: 40px;
        }
        
        .serverDetailsText {
            display: flex;
            flex-direction: column;
            gap: 3px;
        }
        .connected {
            display: flex;
            flex-direction: column;
            gap:10px;
            padding: 5px;
        }
    `;

    static properties = {
        _mcpPath: {state: false},
        _connectedClients: {state: true}
    }

    constructor() {
        super();
        this._mcpPath = null;
        this._connectedClients = null;
    }
    
    connectedCallback() {
        super.connectedCallback();
        this._mcpPath = window.location.origin + basepath.replace("/dev-ui", "/dev-mcp");
        this._getConnectedClients();
        this._observer = this.jsonRpc.getConnectedClientStream().onNext(jsonRpcResponse => { 
            this._getConnectedClients();
        });
    }

    disconnectedCallback() {
        this._observer.cancel();
        super.disconnectedCallback();
    }

    render() {
        if(this._connectedClients){
            return html`<div class="connected">
                            ${this._renderServerDetails()}
                            <vaadin-grid .items="${this._connectedClients}">
                                <vaadin-grid-column path="name"></vaadin-grid-column>
                                <vaadin-grid-column path="version"></vaadin-grid-column>
                            </vaadin-grid>
                        </div>`;
        }else{
            return html`<qwc-no-data message="No MCP Client is connected to Dev MCP."
                                    link="https://quarkus.io/guides/dev-mcp"
                                    linkText="Read more about Dev MCP">
                            ${this._renderServerDetails()}
                </qwc-no-data>
            `;
        }
    }

    hotReload(){
        this._getConnectedClients();
    }
    
    _getConnectedClients(){
        this.jsonRpc.getConnectedClients().then(jsonRpcResponse => {
            this._connectedClients = jsonRpcResponse.result;            
        });
        
        
    }
    
    _renderServerDetails(){
        return html`<div class="serverDetails">
                        <vaadin-icon icon="font-awesome-solid:circle-info"></vaadin-icon>
                        <div class="serverDetailsText">
                            <span>Connect to the Quarkus Dev MCP Server with:</span>
                            <span><b>Protocol:</b> Remote Streamable HTTP</span>
                            <span><b>URL:</b> ${this._mcpPath}</span>
                        <div/>

                </div>`;
    }
}
customElements.define('qwc-dev-mcp-info', QwcDevMCPInfo);