import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import 'qwc-no-data';
import { basepath } from 'devui-data';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/button';
import { RouterController } from 'router-controller';
import { notifier } from 'notifier';
import { msg, updateWhenLocaleChanges, dynamicMsg } from 'localization';

/**
 * This component shows settings for the Dev MCP Server
 */
export class QwcDevMCPSetting extends QwcHotReloadElement {
  jsonRpc = new JsonRpc('devmcp');
  routerController = new RouterController('devmcp');

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
      gap: 10px;
      padding: 5px;
    }
    .unlistedLinks {
      display: flex;
      gap: 30px;
      justify-content: flex-end;
    }
    .unlistedLink {
      cursor: pointer;
    }
    .unlistedLink:hover {
      filter: brightness(150%);
    }
  `;

  static properties = {
    namespace: { type: String },
    _mcpPath: { state: false },
    _configuration: { state: true },
    _connectedClients: { state: true }
  };

  constructor() {
    super();
    updateWhenLocaleChanges(this);
    this._mcpPath = null;
    this._configuration = null;
    this._connectedClients = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._mcpPath = window.location.origin + basepath.replace('/dev-ui', '/dev-mcp');
    this._getConfiguration();
  }

  disconnectedCallback() {
    if (this._observer) {
      this._observer.cancel();
    }
    super.disconnectedCallback();
  }

  render() {
    const readMoreLink = 'https://quarkus.io/guides/dev-mcp';
    const readMoreText = msg('Read more about Dev MCP', { id: 'devmcp-read-more-link' });

    if (this._configuration?.enabled) {
      if (this._connectedClients) {
        const count = this._connectedClients.length;
        return html`
          <qwc-no-data
            .message=${html`${msg('Connected MCP clients', { id: 'devmcp-connected-title' })}: ${count}`}
            .link=${readMoreLink}
            .linkText=${readMoreText}
          >
            <ul>
              ${this._connectedClients.map(
                (client) => html`<li>${client.name} ${client.version}</li>`
              )}
            </ul>
            ${this._renderServerDetails()}
          </qwc-no-data>
          ${this._renderUnlistedPagesLinks()}
        `;
      } else {
        return html`
          <qwc-no-data
            .message=${msg('No MCP client is connected to Dev MCP.', { id: 'devmcp-no-clients' })}
            .link=${readMoreLink}
            .linkText=${readMoreText}
          >
            ${this._renderServerDetails()}
          </qwc-no-data>
          ${this._renderUnlistedPagesLinks()}
        `;
      }
    } else {
      return html`
        <qwc-no-data
          .message=${msg('Dev MCP is not enabled.', { id: 'devmcp-not-enabled' })}
          .link=${readMoreLink}
          .linkText=${readMoreText}
        >
          ${this._renderEnableButton()}
        </qwc-no-data>
      `;
    }
  }

  hotReload() {
    this._getConfiguration();
  }

  _getConfiguration() {
    this.jsonRpc.getMcpServerConfiguration().then((jsonRpcResponse) => {
      this._configuration = jsonRpcResponse.result;
      this._checkConnectionStatus();
    });
  }

  _getConnectedClients() {
    this.jsonRpc.getConnectedClients().then((jsonRpcResponse) => {
      this._connectedClients = jsonRpcResponse.result;
    });
  }

  _renderEnableButton() {
    return html`<vaadin-button theme="primary success" @click=${this._enableDevMcp}>
      ${msg('Enable Dev MCP', { id: 'devmcp-enable' })}
    </vaadin-button>`;
  }

  _renderDisableButton() {
    return html`<vaadin-button theme="primary warning" @click=${this._disableDevMcp}>
      ${msg('Disable Dev MCP', { id: 'devmcp-disable' })}
    </vaadin-button>`;
  }

  _renderServerDetails() {
    return html`<div class="serverDetails">
      <vaadin-icon icon="font-awesome-solid:circle-info"></vaadin-icon>
      <div class="serverDetailsText">
        <span>${msg('Connect to the Quarkus Dev MCP Server with:', { id: 'devmcp-connect-instr' })}</span>
        <span>
          <b>${msg('Protocol:', { id: 'devmcp-protocol-label' })}</b>
          ${msg('Remote Streamable HTTP', { id: 'devmcp-protocol' })}
        </span>
        <span>
          <b>${msg('URL:', { id: 'devmcp-url-label' })}</b>
          ${this._mcpPath}
          <vaadin-button
            theme="tertiary small"
            .title=${msg('Copy to clipboard', { id: 'devmcp-copy-title' })}
            @click=${() => this._copyToClipboard(this._mcpPath)}
          >
            <vaadin-icon icon="font-awesome-solid:clipboard" slot="prefix" class="btn-icon"></vaadin-icon>
          </vaadin-button>
        </span>
      <div/>
    </div>
    ${this._renderDisableButton()}`;
  }

  _renderUnlistedPagesLinks() {
    const unlistedPages = this.routerController.getPagesForNamespace(this.namespace);
    return html`<div class="unlistedLinks">
      ${unlistedPages.map((page) => html`${this._renderUnlistedPageLink(page)}`)}
    </div>`;
  }

  _renderUnlistedPageLink(page) {
    return html`<div class="unlistedLink" style="color:${page.color};" @click=${() => this._navigateToPage(page)}>
      <vaadin-icon icon="${page.icon}"></vaadin-icon> <span>${dynamicMsg('page', page.title)}</span>
    </div>`;
  }

  _navigateToPage(page) {
    window.dispatchEvent(new CustomEvent('close-settings-dialog'));
    this.routerController.go(page);
  }

  _enableDevMcp() {
    this.jsonRpc.enable().then((jsonRpcResponse) => {
      this._configuration = jsonRpcResponse.result;
      this._checkConnectionStatus();
    });
  }

  _disableDevMcp() {
    this.jsonRpc.disable().then(() => {
      this._configuration = null;
      this._connectedClients = null;
    });
  }

  _checkConnectionStatus() {
    if (this._configuration.enabled) {
      this._getConnectedClients();
      this._observer = this.jsonRpc.getConnectedClientStream().onNext(() => {
        this._getConnectedClients();
      });
    }
  }

  _copyToClipboard(txt) {
    navigator.clipboard.writeText(txt).then(
      () => {
        notifier.showInfoMessage(msg('Copied to clipboard.', { id: 'devmcp-copied' }));
      },
      () => {
        notifier.showErrorMessage(msg('Failed to copy to clipboard.', { id: 'devmcp-copy-failed' }));
      }
    );
  }
}
customElements.define('qwc-dev-mcp-setting', QwcDevMCPSetting);
