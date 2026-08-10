import { LitElement, html, css, nothing } from 'lit';
import { Router } from '@vaadin/router';
import '@vaadin/icon';
import { JsonRpc } from '../controller/jsonrpc.js';

/**
 * A single link row on an extension card. Renders an optional icon, the page
 * title (navigates on click) and an optional badge.
 *
 * The badge value is, in order of precedence: a live streaming label, a
 * one-shot dynamic label, or a static label. Dynamic/streaming labels are
 * JSON-RPC method names resolved over the shared read-only channel - they are
 * meant for counts/status only, never secrets.
 *
 * When the page carries an externalUrl the row is a link to that URL (opened in
 * a new tab) rather than an internal SPA route - used, for example, to point at
 * a Swagger UI that is served in production.
 */
export class PuiExtensionLink extends LitElement {

    static styles = css`
        .link {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;
            color: var(--lumo-primary-text-color, #1976d2);
            cursor: pointer;
            padding: 4px 0;
        }
        .link:hover .name {
            text-decoration: underline;
        }
        vaadin-icon {
            width: 16px;
            height: 16px;
            color: var(--lumo-contrast-60pct, #777);
        }
        .name {
            flex: 1;
        }
        vaadin-icon.external {
            width: 12px;
            height: 12px;
            color: var(--lumo-primary-text-color, #1976d2);
        }
        .badge {
            font-size: 11px;
            font-weight: 600;
            line-height: 1;
            padding: 2px 8px;
            border-radius: 10px;
            background: var(--lumo-primary-color-10pct, #e3f2fd);
            color: var(--lumo-primary-text-color, #1976d2);
        }
    `;

    static properties = {
        namespace: {},
        title: {},
        componentLink: {},
        externalUrl: {},
        icon: {},
        tooltip: {},
        staticLabel: {},
        dynamicLabel: {},
        streamingLabel: {},
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
        this._loadLabels();
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        if (this._subscription) {
            this._subscription.cancel();
            this._subscription = null;
        }
    }

    _loadLabels() {
        if (!this.namespace) {
            return;
        }
        if (this.dynamicLabel || this.streamingLabel) {
            const jsonRpc = new JsonRpc(this.namespace);
            if (this.streamingLabel) {
                this._subscription = jsonRpc[this.streamingLabel]()
                    .onNext(response => { this._streamed = response.result; });
            } else if (this.dynamicLabel) {
                jsonRpc[this.dynamicLabel]()
                    .then(response => { this._dynamic = response.result; })
                    .catch(() => { /* badge just stays hidden */ });
            }
        }
    }

    _effectiveLabel() {
        if (this._streamed !== null && this._streamed !== undefined && this._streamed !== '') {
            return this._streamed;
        }
        if (this._dynamic !== null && this._dynamic !== undefined && this._dynamic !== '') {
            return this._dynamic;
        }
        return this.staticLabel && this.staticLabel.trim() !== '' ? this.staticLabel.trim() : null;
    }

    render() {
        const label = this._effectiveLabel();
        const external = !!this.externalUrl;
        return html`
            <span class="link" title=${this.tooltip || ''} @click=${this._navigate}>
                ${this.icon ? html`<vaadin-icon icon=${this.icon}></vaadin-icon>` : nothing}
                <span class="name">${this.title}</span>
                ${external ? html`<vaadin-icon class="external" icon="font-awesome-solid:up-right-from-square"></vaadin-icon>` : nothing}
                ${label !== null ? html`<span class="badge">${label}</span>` : nothing}
            </span>
        `;
    }

    _navigate() {
        // External pages (e.g. a production Swagger UI) open in a new tab; everything
        // else is an internal SPA route rendered by the page host.
        if (this.externalUrl) {
            window.open(this.externalUrl, '_blank', 'noopener,noreferrer');
            return;
        }
        const path = window.location.pathname;
        const prodUiIdx = path.indexOf('/prod-ui');
        const basePath = prodUiIdx >= 0 ? path.substring(0, prodUiIdx + '/prod-ui'.length) : '/q/prod-ui';
        Router.go(basePath + '/' + this.namespace + '/' + this.componentLink);
    }
}
customElements.define('pui-extension-link', PuiExtensionLink);
