import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { basepath } from 'devui-data';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/button';
import '@vaadin/details';
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
    :host {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
    .section {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .sectionHeader {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 1.1em;
      font-weight: bold;
    }
    .sectionDescription {
      color: var(--lumo-secondary-text-color);
      font-size: 0.95em;
      line-height: 1.5;
    }
    .prerequisite {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.85em;
      color: var(--lumo-secondary-text-color);
      padding: 8px 12px;
      background-color: var(--lumo-contrast-5pct);
      border-radius: 4px;
    }
    .serverDetailsText {
      display: flex;
      flex-direction: column;
      gap: 3px;
      padding: 10px 0;
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
    .ideConfigurations {
      width: 100%;
    }
    .ideConfigHeader {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .ideConfigContent {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .configFile {
      font-size: 0.9em;
      color: var(--lumo-secondary-text-color);
    }
    .codeBlock {
      background-color: var(--lumo-contrast-5pct);
      border-radius: 4px;
      padding: 12px;
      font-family: monospace;
      font-size: 0.85em;
      white-space: pre;
      overflow-x: auto;
      position: relative;
    }
    .codeBlockHeader {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 5px;
    }
    .docsLink {
      font-size: 0.85em;
    }
    .docsLink a {
      color: var(--lumo-primary-color);
      text-decoration: none;
    }
    .docsLink a:hover {
      text-decoration: underline;
    }
    .separator {
      border: none;
      border-top: 1px solid var(--lumo-contrast-10pct);
      margin: 10px 0;
    }
    .statusBadge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 0.85em;
      width: fit-content;
    }
    .statusEnabled {
      background-color: var(--lumo-success-color-10pct);
      color: var(--lumo-success-text-color);
    }
    .statusDisabled {
      background-color: var(--lumo-contrast-5pct);
      color: var(--lumo-secondary-text-color);
    }
    .clientList {
      list-style: none;
      padding: 0;
      margin: 5px 0;
    }
    .clientList li {
      padding: 3px 0;
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
    return html`
      ${this._renderAgentMcpSection()}
      <hr class="separator">
      ${this._renderDevMcpSection()}
      ${this._renderUnlistedPagesLinks()}
    `;
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

  _renderAgentMcpSection() {
    return html`
      <div class="section">
        <div class="sectionHeader">
          <vaadin-icon icon="font-awesome-solid:robot"></vaadin-icon>
          <span>${msg('Quarkus Agent MCP', { id: 'devmcp-agent-title' })}</span>
        </div>
        <div class="sectionDescription">
          ${msg('The recommended way to connect an AI coding agent to your Quarkus application. The Quarkus Agent MCP server manages the application lifecycle, proxies Dev MCP tools, provides documentation search, and delivers extension-specific coding skills.', { id: 'devmcp-agent-description' })}
        </div>
        <div class="prerequisite">
          <vaadin-icon icon="font-awesome-solid:circle-info"></vaadin-icon>
          <span>
            ${msg('Requires', { id: 'devmcp-jbang-prereq' })}
            <a href="https://www.jbang.dev/download/" target="_blank" rel="noopener noreferrer">JBang</a>
          </span>
        </div>
        ${this._renderIdeConfigurations(this._getAgentMcpConfigurations())}
      </div>
    `;
  }

  _renderDevMcpSection() {
    const readMoreLink = 'https://quarkus.io/guides/dev-mcp';
    return html`
      <div class="section">
        <div class="sectionHeader">
          <vaadin-icon icon="font-awesome-solid:plug"></vaadin-icon>
          <span>${msg('Dev MCP Server', { id: 'devmcp-server-title' })}</span>
        </div>
        ${this._configuration?.enabled
          ? html`
              <span class="statusBadge statusEnabled">
                <vaadin-icon icon="font-awesome-solid:circle-check"></vaadin-icon>
                ${msg('Enabled', { id: 'devmcp-status-enabled' })}
              </span>
              ${this._connectedClients?.length > 0
                ? html`
                    <span>${msg('Connected MCP clients', { id: 'devmcp-connected-title' })}: ${this._connectedClients.length}</span>
                    <ul class="clientList">
                      ${this._connectedClients.map((client) => html`<li>${client.name} ${client.version}</li>`)}
                    </ul>
                  `
                : html`<span class="sectionDescription">${msg('No MCP client is connected to Dev MCP.', { id: 'devmcp-no-clients' })}</span>`
              }
              ${this._renderDirectConnectionDetails()}
              ${this._renderDisableButton()}
            `
          : html`
              <span class="statusBadge statusDisabled">
                <vaadin-icon icon="font-awesome-solid:circle-xmark"></vaadin-icon>
                ${msg('Not enabled', { id: 'devmcp-status-disabled' })}
              </span>
              <div class="sectionDescription">
                ${msg('Dev MCP provides direct access to development tools on the running application. The Quarkus Agent MCP server will manage this automatically.', { id: 'devmcp-disabled-description' })}
              </div>
              ${this._renderEnableButton()}
            `
        }
        <span class="docsLink">
          <a href="${readMoreLink}" target="_blank" rel="noopener noreferrer">
            ${msg('Read more about Dev MCP', { id: 'devmcp-read-more-link' })}
            <vaadin-icon icon="font-awesome-solid:arrow-up-right-from-square" style="font-size: 0.8em;"></vaadin-icon>
          </a>
        </span>
      </div>
    `;
  }

  _renderDirectConnectionDetails() {
    return html`
      <vaadin-details>
        <div slot="summary" class="ideConfigHeader">
          <vaadin-icon icon="font-awesome-solid:link"></vaadin-icon>
          <span>${msg('Direct connection', { id: 'devmcp-direct-title' })}</span>
        </div>
        <div class="ideConfigContent">
          <div class="sectionDescription">
            ${msg('Connect an MCP client directly to the Dev MCP endpoint on this running application.', { id: 'devmcp-direct-description' })}
          </div>
          <div class="serverDetailsText">
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
          </div>
          ${this._renderIdeConfigurations(this._getDirectMcpConfigurations())}
        </div>
      </vaadin-details>
    `;
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

  _getAgentMcpConfigurations() {
    return [
      {
        name: 'Claude Code',
        icon: 'font-awesome-solid:terminal',
        file: 'Run in terminal',
        docsUrl: 'https://github.com/quarkusio/quarkus-agent-mcp',
        configString: 'claude mcp add quarkus-agent -- jbang quarkus-agent-mcp@quarkusio'
      },
      {
        name: 'OpenCode',
        icon: 'font-awesome-solid:terminal',
        file: 'opencode.json',
        docsUrl: 'https://opencode.ai/docs/mcp-servers/',
        config: {
          mcp: {
            'quarkus-agent': {
              type: 'local',
              command: ['jbang', 'quarkus-agent-mcp@quarkusio']
            }
          }
        }
      },
      {
        name: 'Cline',
        icon: 'font-awesome-solid:terminal',
        file: 'Cline MCP Settings (via UI)',
        docsUrl: 'https://docs.cline.bot/mcp-servers/configuring-mcp-servers',
        config: {
          mcpServers: {
            'quarkus-agent': {
              command: 'jbang',
              args: ['quarkus-agent-mcp@quarkusio'],
              disabled: false
            }
          }
        }
      },
      {
        name: 'Goose',
        icon: 'font-awesome-solid:feather',
        file: '~/.config/goose/config.yaml',
        docsUrl: 'https://block.github.io/goose/docs/getting-started/using-extensions',
        configString: `extensions:
  quarkus-agent:
    name: Quarkus Agent MCP
    type: stdio
    cmd: jbang
    args:
      - quarkus-agent-mcp@quarkusio
    enabled: true`
      },
      {
        name: 'Zed',
        icon: 'font-awesome-solid:bolt',
        file: '~/.config/zed/settings.json',
        docsUrl: 'https://zed.dev/docs/ai/mcp',
        config: {
          context_servers: {
            'quarkus-agent': {
              command: {
                path: 'jbang',
                args: ['quarkus-agent-mcp@quarkusio']
              }
            }
          }
        }
      },
      {
        name: 'VS Code (GitHub Copilot)',
        icon: 'font-awesome-brands:microsoft',
        file: 'User Profile mcp.json (use "MCP: Open User Configuration" command)',
        docsUrl: 'https://code.visualstudio.com/docs/copilot/chat/mcp-servers',
        config: {
          servers: {
            'quarkus-agent': {
              type: 'stdio',
              command: 'jbang',
              args: ['quarkus-agent-mcp@quarkusio']
            }
          }
        }
      },
      {
        name: 'Cursor',
        icon: 'font-awesome-solid:code',
        file: '.cursor/mcp.json',
        docsUrl: 'https://docs.cursor.com/context/mcp',
        config: {
          mcpServers: {
            'quarkus-agent': {
              command: 'jbang',
              args: ['quarkus-agent-mcp@quarkusio']
            }
          }
        }
      },
      {
        name: 'Claude Desktop',
        icon: 'font-awesome-solid:desktop',
        file: '~/Library/Application Support/Claude/claude_desktop_config.json (macOS) or %APPDATA%\\Claude\\claude_desktop_config.json (Windows)',
        docsUrl: 'https://modelcontextprotocol.io/quickstart/user',
        config: {
          mcpServers: {
            'quarkus-agent': {
              command: 'jbang',
              args: ['quarkus-agent-mcp@quarkusio']
            }
          }
        }
      },
      {
        name: 'Windsurf',
        icon: 'font-awesome-solid:wind',
        file: '~/.codeium/windsurf/mcp_config.json',
        docsUrl: 'https://docs.windsurf.com/windsurf/cascade/mcp',
        config: {
          mcpServers: {
            'quarkus-agent': {
              command: 'jbang',
              args: ['quarkus-agent-mcp@quarkusio']
            }
          }
        }
      },
      {
        name: 'JetBrains IDEs',
        icon: 'font-awesome-solid:cube',
        file: 'Settings | Tools | AI Assistant | MCP Servers',
        docsUrl: 'https://www.jetbrains.com/help/idea/mcp-server.html',
        config: {
          servers: {
            'quarkus-agent': {
              type: 'stdio',
              command: 'jbang',
              args: ['quarkus-agent-mcp@quarkusio']
            }
          }
        }
      }
    ];
  }

  _getDirectMcpConfigurations() {
    return [
      {
        name: 'OpenCode',
        icon: 'font-awesome-solid:terminal',
        file: 'opencode.json',
        docsUrl: 'https://opencode.ai/docs/mcp-servers/',
        config: {
          mcp: {
            'quarkus-mcp': {
              type: 'remote',
              url: this._mcpPath
            }
          }
        }
      },
      {
        name: 'Cline',
        icon: 'font-awesome-solid:terminal',
        file: 'Cline MCP Settings (via UI)',
        docsUrl: 'https://docs.cline.bot/mcp-servers/configuring-mcp-servers',
        config: {
          mcpServers: {
            'quarkus-mcp': {
              url: this._mcpPath,
              disabled: false
            }
          }
        }
      },
      {
        name: 'Goose',
        icon: 'font-awesome-solid:feather',
        file: '~/.config/goose/config.yaml',
        docsUrl: 'https://block.github.io/goose/docs/getting-started/using-extensions',
        configString: `extensions:
  quarkus-mcp:
    name: Quarkus MCP
    type: streamable_http
    uri: ${this._mcpPath}
    enabled: true`
      },
      {
        name: 'Zed',
        icon: 'font-awesome-solid:bolt',
        file: '~/.config/zed/settings.json',
        docsUrl: 'https://zed.dev/docs/ai/mcp',
        prerequisite: 'Requires Node.js and npx',
        config: {
          context_servers: {
            'quarkus-mcp': {
              command: {
                path: 'npx',
                args: ['-y', 'mcp-remote', this._mcpPath]
              }
            }
          }
        }
      },
      {
        name: 'VS Code (GitHub Copilot)',
        icon: 'font-awesome-brands:microsoft',
        file: 'User Profile mcp.json (use "MCP: Open User Configuration" command)',
        docsUrl: 'https://code.visualstudio.com/docs/copilot/chat/mcp-servers',
        config: {
          servers: {
            'quarkus-mcp': {
              url: this._mcpPath
            }
          }
        }
      },
      {
        name: 'Cursor',
        icon: 'font-awesome-solid:code',
        file: '~/.cursor/mcp.json',
        docsUrl: 'https://docs.cursor.com/context/mcp',
        config: {
          mcpServers: {
            'quarkus-mcp': {
              url: this._mcpPath
            }
          }
        }
      },
      {
        name: 'Claude Desktop',
        icon: 'font-awesome-solid:desktop',
        file: '~/Library/Application Support/Claude/claude_desktop_config.json (macOS) or %APPDATA%\\Claude\\claude_desktop_config.json (Windows)',
        docsUrl: 'https://modelcontextprotocol.io/quickstart/user',
        prerequisite: 'Requires Node.js and npx',
        config: {
          mcpServers: {
            'quarkus-mcp': {
              command: 'npx',
              args: ['-y', 'mcp-remote', this._mcpPath]
            }
          }
        }
      },
      {
        name: 'Windsurf',
        icon: 'font-awesome-solid:wind',
        file: '~/.codeium/windsurf/mcp_config.json',
        docsUrl: 'https://docs.windsurf.com/windsurf/cascade/mcp',
        config: {
          mcpServers: {
            'quarkus-mcp': {
              serverUrl: this._mcpPath
            }
          }
        }
      },
      {
        name: 'JetBrains IDEs',
        icon: 'font-awesome-solid:cube',
        file: 'Settings | Tools | MCP Server (auto-configure available)',
        docsUrl: 'https://www.jetbrains.com/help/idea/mcp-server.html',
        config: {
          mcpServers: {
            'quarkus-mcp': {
              url: this._mcpPath
            }
          }
        }
      }
    ];
  }

  _renderIdeConfigurations(configs) {
    return html`
      <div class="ideConfigurations">
        ${configs.map((ide) => this._renderIdeConfigPanel(ide))}
      </div>
    `;
  }

  _renderIdeConfigPanel(ide) {
    const configText = ide.configString || JSON.stringify(ide.config, null, 2);
    return html`
      <vaadin-details>
        <div slot="summary" class="ideConfigHeader">
          <vaadin-icon icon="${ide.icon}"></vaadin-icon>
          <span>${ide.name}</span>
        </div>
        <div class="ideConfigContent">
          <div class="configFile">
            <b>${msg('Configuration file:', { id: 'devmcp-config-file' })}</b> ${ide.file}
          </div>
          ${ide.prerequisite
            ? html`
                <div class="prerequisite">
                  <vaadin-icon icon="font-awesome-solid:circle-info"></vaadin-icon>
                  <span>${ide.prerequisite}</span>
                </div>
              `
            : ''}
          <div class="codeBlockHeader">
            <span class="docsLink">
              <a href="${ide.docsUrl}" target="_blank" rel="noopener noreferrer">
                ${msg('Documentation', { id: 'devmcp-docs-link' })}
                <vaadin-icon icon="font-awesome-solid:arrow-up-right-from-square" style="font-size: 0.8em;"></vaadin-icon>
              </a>
            </span>
            <vaadin-button
              theme="tertiary small"
              .title=${msg('Copy configuration', { id: 'devmcp-copy-config' })}
              @click=${() => this._copyToClipboard(configText)}
            >
              <vaadin-icon icon="font-awesome-solid:clipboard" slot="prefix"></vaadin-icon>
              ${msg('Copy', { id: 'devmcp-copy' })}
            </vaadin-button>
          </div>
          <div class="codeBlock">${configText}</div>
        </div>
      </vaadin-details>
    `;
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
