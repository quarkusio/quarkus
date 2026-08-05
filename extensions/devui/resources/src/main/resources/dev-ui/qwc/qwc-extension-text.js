import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';
import { StorageController } from 'storage-controller';
import '@vaadin/icon';
import '@qomponent/qui-badge';

export class QwcExtensionText extends QwcHotReloadElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            color: var(--lumo-contrast-70pct);
            font-size: var(--lumo-font-size-s);
            padding: 2px 5px;
            gap: 5px;
        }
        .textRow {
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            color: var(--lumo-contrast-70pct);
            font-size: var(--lumo-font-size-s);
            padding: 4px 8px;
            gap: 5px;
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
        staticText: { type: String },
        dynamicText: { type: String },
        streamingText: { type: String },
        streamingTextParams: { type: String },
        _effectiveText: { state: true },
        _observer: { state: false }
    };

    constructor() {
        super();
        this._effectiveText = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload() {
        if (this._observer) {
            this._observer.cancel();
        }

        this._effectiveText = null;
        if (this.streamingText) {
            this.jsonRpc = new JsonRpc(this);
            if (this.streamingTextParams) {
                let streamingTextParamsArray = this.streamingTextParams.split(',');
                let storageController = new StorageController(this.namespace);
                let params = {};
                for (const localParam of streamingTextParamsArray) {
                    let val = storageController.get(localParam);
                    if (!val) val = "";
                    params[localParam] = val;
                }
                this._observer = this.jsonRpc[this.streamingText](params).onNext(jsonRpcResponse => {
                    this._effectiveText = jsonRpcResponse.result;
                    this.requestUpdate();
                });
            } else {
                this._observer = this.jsonRpc[this.streamingText]().onNext(jsonRpcResponse => {
                    this._effectiveText = jsonRpcResponse.result;
                    this.requestUpdate();
                });
            }
        } else if (this.dynamicText) {
            this.jsonRpc = new JsonRpc(this);
            this.jsonRpc[this.dynamicText]().then(jsonRpcResponse => {
                this._effectiveText = jsonRpcResponse.result;
                this.requestUpdate();
            });
        } else if (this.staticText) {
            this._effectiveText = this.staticText;
        }
    }

    disconnectedCallback() {
        if (this._observer) {
            this._observer.cancel();
        }
        super.disconnectedCallback();
    }

    render() {
        return html`
            <div class="textRow">
                <span class="iconAndName">
                    ${this.iconName && this.iconName !== 'null'
                        ? html`<vaadin-icon class="icon" icon="${this.iconName}"></vaadin-icon>`
                        : ''}
                    ${this.displayName && this.displayName !== 'null' ? this.displayName : ''}
                </span>
            </div>
            ${this._renderText()}
        `;
    }

    _renderText() {
        if (this._effectiveText !== null) {
            return html`<qui-badge tiny pill><span>${this._effectiveText}</span></qui-badge>`;
        }
    }
}
customElements.define('qwc-extension-text', QwcExtensionText);
