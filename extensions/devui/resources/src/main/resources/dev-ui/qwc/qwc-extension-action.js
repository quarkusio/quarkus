import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { notifier } from 'notifier';
import '@vaadin/icon';

export class QwcExtensionAction extends LitElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: row;
            align-items: center;
            padding: 2px 5px;
        }
        .actionButton {
            display: flex;
            flex-direction: row;
            justify-content: flex-start;
            align-items: center;
            color: var(--lumo-contrast-70pct);
            font-size: var(--lumo-font-size-s);
            padding: 2px 5px;
            cursor: pointer;
            gap: 5px;
            border-radius: var(--devui-radius-sm, 6px);
            border: none;
            background: none;
            font-family: inherit;
            transition: background-color var(--devui-transition-fast, 0.15s ease),
                        color var(--devui-transition-fast, 0.15s ease);
        }
        .actionButton:hover {
            background-color: var(--lumo-primary-color-10pct);
            color: var(--lumo-primary-text-color);
        }
        .actionButton:disabled {
            opacity: 0.5;
            cursor: wait;
        }
        .icon {
            padding-right: 5px;
            width: var(--lumo-icon-size-s);
            height: var(--lumo-icon-size-s);
        }
        .iconAndName {
            display: flex;
            flex-direction: row;
            justify-content: flex-start;
            align-items: center;
        }
    `;

    static properties = {
        namespace: { type: String },
        displayName: { type: String },
        iconName: { type: String },
        tooltipContent: { type: String },
        actionType: { type: String },
        actionReference: { type: String },
        showResultNotification: { type: Boolean },
        _loading: { state: true }
    };

    constructor() {
        super();
        this._loading = false;
        this.showResultNotification = true;
    }

    render() {
        return html`
            <button class="actionButton"
                    @click="${this._handleClick}"
                    title="${this.tooltipContent || ''}"
                    ?disabled="${this._loading}">
                <span class="iconAndName">
                    ${this.iconName && this.iconName !== 'null'
                        ? html`<vaadin-icon class="icon" icon="${this.iconName}"></vaadin-icon>`
                        : ''}
                    ${this.displayName}
                </span>
            </button>
        `;
    }

    _handleClick() {
        if (this.actionType === 'JSONRPC') {
            this._executeJsonRpc();
        } else if (this.actionType === 'URL') {
            this._executeUrl();
        }
    }

    _executeJsonRpc() {
        this._loading = true;
        const jrpc = new JsonRpc(this.namespace);
        jrpc[this.actionReference]().then(jsonRpcResponse => {
            this._loading = false;
            if (this.showResultNotification) {
                const result = jsonRpcResponse.result;
                const message = typeof result === 'string'
                    ? result
                    : JSON.stringify(result);
                notifier.showSuccessMessage(message);
            }
        }).catch(error => {
            this._loading = false;
            notifier.showErrorMessage(
                this.displayName + ' failed: ' + (error?.error?.message || error)
            );
        });
    }

    _executeUrl() {
        this._loading = true;
        fetch(this.actionReference)
            .then(response => {
                this._loading = false;
                if (response.ok) {
                    if (this.showResultNotification) {
                        notifier.showSuccessMessage('OK');
                    }
                } else {
                    notifier.showErrorMessage(
                        this.displayName + ' failed: ' + response.statusText
                    );
                }
            })
            .catch(error => {
                this._loading = false;
                notifier.showErrorMessage(
                    this.displayName + ' failed: ' + error
                );
            });
    }
}
customElements.define('qwc-extension-action', QwcExtensionAction);
