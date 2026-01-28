import { LitElement, html, css } from 'lit';
import '@vaadin/button';
import '@vaadin/icon';

/**
 * Dev UI page providing an embedded xterm.js terminal connected via WebSocket.
 * Uses the same JSON protocol as the standalone index.html (init, read, resize actions).
 *
 * Uses createRenderRoot() { return this; } to avoid Shadow DOM complications with xterm.js CSS.
 */
export class QwcAeshTerminal extends LitElement {

    static properties = {
        _connected: { state: true },
        _connecting: { state: true }
    };

    // Render into light DOM so xterm.js CSS works correctly
    createRenderRoot() {
        return this;
    }

    constructor() {
        super();
        this._connected = false;
        this._connecting = false;
        this._term = null;
        this._fitAddon = null;
        this._socket = null;
        this._resizeHandler = null;
        this._resizeObserver = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this._loadXtermResources();
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this._disconnect();
        this._cleanupResizeHandlers();
    }

    render() {
        // Inline styles since we're in light DOM
        return html`
            <style>
                .aesh-terminal-wrapper {
                    display: flex;
                    flex-direction: column;
                    height: 100%;
                    gap: 10px;
                    padding: 10px;
                }
                .aesh-terminal-toolbar {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                }
                .aesh-terminal-status {
                    font-size: 14px;
                    padding: 4px 12px;
                    border-radius: 4px;
                }
                .aesh-status-connected {
                    background: var(--lumo-success-color-10pct, #1a472a);
                    color: var(--lumo-success-text-color, #4ade80);
                }
                .aesh-status-disconnected {
                    background: var(--lumo-error-color-10pct, #4a1a1a);
                    color: var(--lumo-error-text-color, #f87171);
                }
                .aesh-status-connecting {
                    background: var(--lumo-contrast-10pct, #4a3a1a);
                    color: var(--lumo-contrast-80pct, #fbbf24);
                }
                .aesh-terminal-container {
                    flex: 1;
                    min-height: 0;
                    border: 1px solid var(--lumo-contrast-20pct, #444);
                    border-radius: 4px;
                    overflow: hidden;
                    background: #000;
                }
            </style>
            <div class="aesh-terminal-wrapper">
                <div class="aesh-terminal-toolbar">
                    ${this._connected
                        ? html`<vaadin-button theme="small" @click="${this._disconnect}">
                                    <vaadin-icon icon="font-awesome-solid:plug-circle-xmark"></vaadin-icon>
                                    Disconnect
                                </vaadin-button>`
                        : html`<vaadin-button theme="small primary" @click="${this._connect}"
                                    ?disabled="${this._connecting}">
                                    <vaadin-icon icon="font-awesome-solid:plug"></vaadin-icon>
                                    Connect
                                </vaadin-button>`
                    }
                    ${this._renderStatus()}
                </div>
                <div id="aesh-terminal-container" class="aesh-terminal-container"></div>
            </div>
        `;
    }

    _renderStatus() {
        if (this._connecting) {
            return html`<span class="aesh-terminal-status aesh-status-connecting">Connecting...</span>`;
        }
        if (this._connected) {
            return html`<span class="aesh-terminal-status aesh-status-connected">Connected</span>`;
        }
        return html`<span class="aesh-terminal-status aesh-status-disconnected">Disconnected</span>`;
    }

    async _loadXtermResources() {
        // Load xterm.js CSS
        if (!document.querySelector('link[href*="xterm"]')) {
            const link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = 'https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/css/xterm.css';
            document.head.appendChild(link);
        }

        // Load xterm.js
        if (!window.Terminal) {
            await this._loadScript('https://cdn.jsdelivr.net/npm/@xterm/xterm@5.5.0/lib/xterm.js');
        }

        // Load fit addon
        if (!window.FitAddon) {
            await this._loadScript('https://cdn.jsdelivr.net/npm/@xterm/addon-fit@0.10.0/lib/addon-fit.js');
        }
    }

    _loadScript(src) {
        return new Promise((resolve, reject) => {
            const existing = document.querySelector(`script[src="${src}"]`);
            if (existing) {
                resolve();
                return;
            }
            const script = document.createElement('script');
            script.src = src;
            script.onload = resolve;
            script.onerror = reject;
            document.head.appendChild(script);
        });
    }

    async _connect() {
        if (this._connected || this._connecting) return;

        this._connecting = true;
        this.requestUpdate();

        try {
            await this._loadXtermResources();
        } catch (e) {
            console.error('Failed to load xterm.js resources:', e);
            this._connecting = false;
            return;
        }

        // Determine WebSocket URL
        const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsHost = window.location.host || 'localhost:8080';
        const wsPath = this.jsondata?.websocketPath || '/aesh/terminal';
        const wsUrl = `${wsProtocol}//${wsHost}${wsPath}`;

        this._socket = new WebSocket(wsUrl);

        this._socket.onopen = () => {
            this._connecting = false;
            this._connected = true;
            this.requestUpdate();

            // Wait for DOM update then init terminal
            this.updateComplete.then(() => {
                this._initTerminal();
            });
        };

        this._socket.onerror = (error) => {
            console.error('WebSocket error:', error);
            this._connecting = false;
            this._connected = false;
            this.requestUpdate();
        };

        this._socket.onclose = (event) => {
            if (this._term) {
                this._term.write('\r\n\x1b[31mConnection closed.\x1b[0m\r\n');
            }
            this._connected = false;
            this._connecting = false;
            this.requestUpdate();
        };
    }

    _initTerminal() {
        const container = this.querySelector('#aesh-terminal-container') ||
                          document.getElementById('aesh-terminal-container');
        if (!container) return;

        // Clear any previous terminal
        container.innerHTML = '';

        this._term = new window.Terminal({
            cursorBlink: true,
            cursorStyle: 'block',
            fontFamily: '"DejaVu Sans Mono", "Liberation Mono", "Courier New", monospace',
            fontSize: 14,
            theme: {
                background: '#000000',
                foreground: '#f0f0f0',
                cursor: '#f0f0f0',
                cursorAccent: '#000000',
                selectionBackground: 'rgba(255, 255, 255, 0.3)'
            },
            allowProposedApi: true
        });

        this._fitAddon = new window.FitAddon.FitAddon();
        this._term.loadAddon(this._fitAddon);
        this._term.open(container);

        // Delay fit to ensure container has dimensions
        requestAnimationFrame(() => {
            this._fitAddon.fit();

            // Send init message
            this._socket.send(JSON.stringify({
                action: 'init',
                type: 'xterm-256color',
                colorDepth: 'TRUE_COLOR',
                features: ['UNICODE'],
                cols: this._term.cols,
                rows: this._term.rows,
                userAgent: navigator.userAgent
            }));
        });

        // Receive terminal output
        this._socket.onmessage = (event) => {
            if (event.type === 'message' && this._term) {
                this._term.write(event.data);
            }
        };

        // Send user input
        this._term.onData((data) => {
            if (this._socket && this._socket.readyState === WebSocket.OPEN) {
                this._socket.send(JSON.stringify({ action: 'read', data: data }));
            }
        });

        // Send resize events
        this._term.onResize((size) => {
            if (this._socket && this._socket.readyState === WebSocket.OPEN) {
                this._socket.send(JSON.stringify({
                    action: 'resize',
                    cols: size.cols,
                    rows: size.rows
                }));
            }
        });

        // Handle window resize
        let resizeTimeout;
        this._resizeHandler = () => {
            clearTimeout(resizeTimeout);
            resizeTimeout = setTimeout(() => {
                if (this._fitAddon && this._term) {
                    this._fitAddon.fit();
                }
            }, 100);
        };
        window.addEventListener('resize', this._resizeHandler);

        // Handle container resize
        if (window.ResizeObserver) {
            this._resizeObserver = new ResizeObserver(() => {
                if (this._resizeHandler) {
                    this._resizeHandler();
                }
            });
            this._resizeObserver.observe(container);
        }

        this._term.focus();
    }

    _disconnect() {
        this._cleanupResizeHandlers();

        if (this._socket) {
            this._socket.onclose = null;
            this._socket.onerror = null;
            this._socket.onmessage = null;
            this._socket.close();
            this._socket = null;
        }

        if (this._term) {
            this._term.dispose();
            this._term = null;
            this._fitAddon = null;
        }

        this._connected = false;
        this._connecting = false;
        this.requestUpdate();
    }

    _cleanupResizeHandlers() {
        if (this._resizeHandler) {
            window.removeEventListener('resize', this._resizeHandler);
            this._resizeHandler = null;
        }
        if (this._resizeObserver) {
            this._resizeObserver.disconnect();
            this._resizeObserver = null;
        }
    }
}

customElements.define('qwc-aesh-terminal', QwcAeshTerminal);
