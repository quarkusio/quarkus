import { LitElement, html, css, nothing } from 'lit';
import '@vaadin/icon';
import { JsonRpc } from '../controller/jsonrpc.js';

/**
 * A labelled text row on an extension card (e.g. "Entries  1,204"). The value is,
 * in order of precedence, a live streaming text, a one-shot dynamic text, or a
 * static text. Dynamic/streaming values are JSON-RPC method names resolved over
 * the shared read-only channel - counts/status only, never secrets.
 */
export class PuiExtensionText extends LitElement {

    static styles = css`
        .text {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 13px;
            color: var(--lumo-secondary-text-color, #666);
            padding: 3px 0;
        }
        vaadin-icon {
            width: 14px;
            height: 14px;
            color: var(--lumo-contrast-50pct, #999);
        }
        .title {
            flex: 1;
        }
        .value {
            font-weight: 600;
            color: var(--lumo-body-text-color, #333);
        }
    `;

    static properties = {
        namespace: {},
        title: {},
        icon: {},
        staticText: {},
        dynamicText: {},
        streamingText: {},
        _dynamic: { state: true },
        _streamed: { state: true }
    };

    constructor() {
        super();
        this._dynamic = null;
        this._streamed = null;
        this._subscription = null;
    }

    connectedCallback() {
        super.connectedCallback();
        if (!this.namespace) {
            return;
        }
        if (this.streamingText) {
            const jsonRpc = new JsonRpc(this.namespace);
            this._subscription = jsonRpc[this.streamingText]()
                .onNext(response => { this._streamed = response.result; });
        } else if (this.dynamicText) {
            const jsonRpc = new JsonRpc(this.namespace);
            jsonRpc[this.dynamicText]()
                .then(response => { this._dynamic = response.result; })
                .catch(() => { /* value just stays hidden */ });
        }
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        if (this._subscription) {
            this._subscription.cancel();
            this._subscription = null;
        }
    }

    _effectiveValue() {
        if (this._streamed !== null && this._streamed !== undefined && this._streamed !== '') {
            return this._streamed;
        }
        if (this._dynamic !== null && this._dynamic !== undefined && this._dynamic !== '') {
            return this._dynamic;
        }
        return this.staticText && this.staticText.trim() !== '' ? this.staticText.trim() : null;
    }

    render() {
        const value = this._effectiveValue();
        return html`
            <div class="text">
                ${this.icon ? html`<vaadin-icon icon=${this.icon}></vaadin-icon>` : nothing}
                <span class="title">${this.title || ''}</span>
                ${value !== null ? html`<span class="value">${value}</span>` : nothing}
            </div>
        `;
    }
}
customElements.define('pui-extension-text', PuiExtensionText);
