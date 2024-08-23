import { LitElement, html, css} from 'lit';
import { pages } from 'build-time-data';
import { JsonRpc } from 'jsonrpc';
import 'qwc-extension-link';

const NAME = "Reactive Messaging - RabbitMQ";
export class QwcRabbitMqCard extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
      .identity {
        display: flex;
        justify-content: flex-start;
      }
      .description {
        padding-bottom: 10px;
      }
      .logo {
        padding-bottom: 10px;
        margin-right: 5px;
      }
      .card-content {
        color: var(--lumo-contrast-90pct);
        display: flex;
        flex-direction: column;
        justify-content: flex-start;
        padding: 2px 2px;
        height: 100%;
      }
      .card-content slot {
        display: flex;
        flex-flow: column wrap;
        padding-top: 5px;
      }
    `;

    static properties = {
        extensionName: {attribute: true},
        description: {attribute: true},
        guide: {attribute: true},
        namespace: {attribute: true},
        _port: {state: true},
        _externalUrl: {state: true}
    };

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getRabbitMqPort().then(jsonRpcResponse => {
            this._port = jsonRpcResponse.result;
            this._externalUrl = "http://localhost:" + this._port;
        });
    }

    render() {
        return html`<div class="card-content" slot="content">
            <div class="identity">
                <div class="logo">
                    <img src="data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjM2MiIgaGVpZ2h0PSIyNTAwIiB2aWV3Qm94PSIwIDAgMjU2IDI3MSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiBwcmVzZXJ2ZUFzcGVjdFJhdGlvPSJ4TWlkWU1pZCI+PHBhdGggZD0iTTI0NS40NCAxMDguMzA4aC04NS4wOWE3LjczOCA3LjczOCAwIDAgMS03LjczNS03LjczNHYtODguNjhDMTUyLjYxNSA1LjMyNyAxNDcuMjkgMCAxNDAuNzI2IDBoLTMwLjM3NWMtNi41NjggMC0xMS44OSA1LjMyNy0xMS44OSAxMS44OTR2ODguMTQzYzAgNC41NzMtMy42OTcgOC4yOS04LjI3IDguMzFsLTI3Ljg4NS4xMzNjLTQuNjEyLjAyNS04LjM1OS0zLjcxNy04LjM1LTguMzI1bC4xNzMtODguMjQxQzU0LjE0NCA1LjMzNyA0OC44MTcgMCA0Mi4yNCAwSDExLjg5QzUuMzIxIDAgMCA1LjMyNyAwIDExLjg5NFYyNjAuMjFjMCA1LjgzNCA0LjcyNiAxMC41NiAxMC41NTUgMTAuNTZIMjQ1LjQ0YzUuODM0IDAgMTAuNTYtNC43MjYgMTAuNTYtMTAuNTZWMTE4Ljg2OGMwLTUuODM0LTQuNzI2LTEwLjU2LTEwLjU2LTEwLjU2em0tMzkuOTAyIDkzLjIzM2MwIDcuNjQ1LTYuMTk4IDEzLjg0NC0xMy44NDMgMTMuODQ0SDE2Ny42OWMtNy42NDYgMC0xMy44NDQtNi4xOTktMTMuODQ0LTEzLjg0NHYtMjQuMDA1YzAtNy42NDYgNi4xOTgtMTMuODQ0IDEzLjg0NC0xMy44NDRoMjQuMDA1YzcuNjQ1IDAgMTMuODQzIDYuMTk4IDEzLjg0MyAxMy44NDR2MjQuMDA1eiIgZmlsbD0iI0Y2MCIvPjwvc3ZnPgo="
                                       alt="${this.extensionName}" 
                                       title="${this.extensionName}"
                                       width="32" 
                                       height="32">
                </div>
                <div class="description">${this.description}</div>
            </div>
            ${this._renderCardLinks()}
        </div>
        `;
    }

    _renderCardLinks(){
        if(this._port){
            return html`
                <qwc-extension-link slot="link"
                    namespace="${this.namespace}"
                    extensionName="${this.extensionName}"
                    iconName="font-awesome-solid:arrow-up-right-from-square"
                    displayName="RabbitMQ Management UI"
                    staticLabel="${this._port}"
                    ?embed=false
                    externalUrl="${this._externalUrl}">
                </qwc-extension-link>
            `;
        }
    }

}
customElements.define('qwc-rabbitmq-card', QwcRabbitMqCard);