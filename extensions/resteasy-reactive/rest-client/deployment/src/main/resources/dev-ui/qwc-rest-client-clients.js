import { LitElement, html, css} from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/icon';
import '@vaadin/progress-bar';
import '@vaadin/checkbox';
import '@vaadin/grid';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/grid/vaadin-grid-sort-column.js';

export class QwcRestClientClients extends LitElement {

    jsonRpc = new JsonRpc(this);

    // Component style
    static styles = css`
        :host {
          display: block;
          height: 100%;
        }
        .datatable {
          height: 100%;
        }
        code {
            font-size: 85%;
        }`;

    // Component properties
    static properties = {
        _clients: {state: true}
    }
    
    constructor() {
        super();
        this._clients = null;
    }

    // Components callbacks

    /**
     * Called when displayed
     */
    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getAll().then(jsonRpcResponse => {
            this._clients = jsonRpcResponse.result;
        });
    }

    /**
     * Called when it needs to render the components
     * @returns {*}
     */
    render() {
        if (this._clients) {
            return this._renderClientsTable();
        } else {
            return html`<span>Loading REST Clients...</span>`;
        }
    }

    // View / Templates

    _renderClientsTable() {
        return html`
                <vaadin-grid .items="${this._clients}" class="datatable" theme="no-border">
                    <vaadin-grid-column auto-width
                                        header="Client interface"
                                        ${columnBodyRenderer(this._clientInterfaceRenderer, [])}
                                        resizable>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                                        header="Config Key"
                                        ${columnBodyRenderer(this._configKeyRenderer, [])}
                                        resizable>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                                        header="Is CDI Bean"
                                        ${columnBodyRenderer(this._isBeanRenderer, [])}
                                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>`;
    }

  _clientInterfaceRenderer(client){
       return html`
         <code>${client.clientInterface}</code>
       `;
  }

    _configKeyRenderer(client){
         return html`
           <code>${client.configKey}</code>
         `;
    }

  _isBeanRenderer(client){
    if(client.isBean !== false){
      return html`
        <qui-badge level="success" icon="font-awesome-solid:check"></qui-badge>
      `;
    }
  }

}
customElements.define('qwc-rest-client-clients', QwcRestClientClients);
