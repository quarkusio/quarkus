import { LitElement, html, css } from 'lit';

/**
 * Shared, consistent placeholder for the three "nothing to show yet" states a
 * Prod UI view can be in: still loading, loaded but empty, or unavailable
 * (the backing data could not be reached). Using one component keeps the empty
 * and error presentation identical across the landing page and every built-in
 * page, instead of each view rolling its own ad-hoc "Loading..." span.
 *
 * Purely presentational and read-only - it renders text passed to it and never
 * calls back to the server.
 */
export class PuiEmptyState extends LitElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 10px;
            padding: 48px 24px;
            text-align: center;
            color: var(--lumo-secondary-text-color, #666);
        }
        .icon {
            font-size: 34px;
            line-height: 1;
            opacity: 0.6;
        }
        .icon.unavailable {
            color: var(--lumo-error-text-color, #c62828);
            opacity: 0.8;
        }
        .heading {
            font-size: 16px;
            font-weight: 600;
            color: var(--lumo-body-text-color, #333);
        }
        .message {
            font-size: 14px;
            max-width: 420px;
        }
        .spinner {
            width: 26px;
            height: 26px;
            border: 3px solid var(--lumo-contrast-10pct, #e0e0e0);
            border-top-color: var(--lumo-primary-color, #1976d2);
            border-radius: 50%;
            animation: pui-spin 0.8s linear infinite;
        }
        @keyframes pui-spin {
            to { transform: rotate(360deg); }
        }
        ::slotted(*) {
            margin-top: 4px;
        }
    `;

    static properties = {
        kind: { type: String },
        heading: { type: String },
        message: { type: String }
    };

    constructor() {
        super();
        this.kind = 'empty';
        this.heading = '';
        this.message = '';
    }

    render() {
        return html`
            ${this._renderIcon()}
            ${this.heading ? html`<div class="heading">${this.heading}</div>` : ''}
            ${this.message ? html`<div class="message">${this.message}</div>` : ''}
            <slot></slot>
        `;
    }

    _renderIcon() {
        if (this.kind === 'loading') {
            return html`<div class="spinner" aria-label="Loading"></div>`;
        }
        if (this.kind === 'unavailable') {
            return html`<div class="icon unavailable" aria-hidden="true">&#9888;</div>`;
        }
        return html`<div class="icon" aria-hidden="true">&#8709;</div>`;
    }
}
customElements.define('pui-empty-state', PuiEmptyState);
