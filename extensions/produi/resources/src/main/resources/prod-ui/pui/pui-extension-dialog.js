import { LitElement, html, css, nothing } from 'lit';
import { Router } from '@vaadin/router';
import '@vaadin/icon';

/**
 * The "back of card" details for an extension, shown as a modal overlay. Everything here is
 * non-sensitive build-time metadata (description, guide, categories, keywords, library versions).
 * The Configuration action deep-links to the built-in Configuration page pre-filtered to this
 * extension's config prefixes - which is served, read-only and secret-masked, by the backend.
 */
export class PuiExtensionDialog extends LitElement {

    static styles = css`
        .backdrop {
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.45);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 1000;
        }
        .dialog {
            background: var(--lumo-base-color, #fff);
            border-radius: 10px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.25);
            width: min(560px, calc(100vw - 32px));
            max-height: calc(100vh - 64px);
            overflow-y: auto;
            padding: 24px;
        }
        .head {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 4px;
        }
        .title {
            font-size: 18px;
            font-weight: 600;
            flex: 1;
            color: var(--lumo-header-text-color, #1a1a1a);
        }
        .close {
            cursor: pointer;
            color: var(--lumo-secondary-text-color, #666);
            width: 20px;
            height: 20px;
        }
        .close:hover {
            color: var(--lumo-body-text-color, #333);
        }
        .status {
            font-size: 10px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.03em;
            padding: 2px 7px;
            border-radius: 4px;
            background: var(--lumo-contrast-10pct, #eee);
            color: var(--lumo-secondary-text-color, #666);
        }
        .description {
            font-size: 14px;
            line-height: 1.5;
            color: var(--lumo-body-text-color, #333);
            margin: 12px 0 16px;
        }
        .row {
            display: flex;
            gap: 10px;
            padding: 8px 0;
            border-top: 1px solid var(--lumo-contrast-5pct, #f0f0f0);
            font-size: 13px;
        }
        .row .label {
            width: 110px;
            flex-shrink: 0;
            color: var(--lumo-secondary-text-color, #666);
            font-weight: 500;
        }
        .row .val {
            flex: 1;
            color: var(--lumo-body-text-color, #333);
            word-break: break-word;
        }
        .row .val a {
            color: var(--lumo-primary-text-color, #1976d2);
            text-decoration: none;
            cursor: pointer;
        }
        .row .val a:hover {
            text-decoration: underline;
        }
        .badges {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }
        .badge {
            font-size: 11px;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 10px;
            background: var(--lumo-primary-color-10pct, #e3f2fd);
            color: var(--lumo-primary-text-color, #1976d2);
        }
        .badge.clickable {
            cursor: pointer;
        }
        .actions {
            display: flex;
            justify-content: flex-end;
            gap: 8px;
            margin-top: 20px;
        }
        button {
            font-size: 13px;
            font-weight: 500;
            padding: 8px 16px;
            border-radius: 6px;
            border: 1px solid var(--lumo-contrast-20pct, #ccc);
            background: var(--lumo-base-color, #fff);
            color: var(--lumo-body-text-color, #333);
            cursor: pointer;
        }
        button.primary {
            background: var(--lumo-primary-color, #1976d2);
            border-color: var(--lumo-primary-color, #1976d2);
            color: var(--lumo-primary-contrast-color, #fff);
        }
        button:hover {
            filter: brightness(0.97);
        }
    `;

    static properties = {
        ext: {},
        basePath: {}
    };

    render() {
        const ext = this.ext;
        if (!ext) {
            return nothing;
        }
        const cfg = ext.configPrefixes || [];
        return html`
            <div class="backdrop" @click=${this._onBackdrop}>
                <div class="dialog" @click=${e => e.stopPropagation()}>
                    <div class="head">
                        <span class="title">${ext.title || ext.namespace}</span>
                        ${ext.status ? html`<span class="status">${ext.status}</span>` : nothing}
                        <vaadin-icon class="close" icon="font-awesome-solid:xmark"
                            @click=${this._close}></vaadin-icon>
                    </div>
                    ${ext.description ? html`<div class="description">${ext.description}</div>` : nothing}

                    ${this._row('Extension', ext.namespace)}
                    ${ext.guide ? this._row('Guide', html`
                        <a href=${ext.guide} target="_blank" rel="noopener noreferrer">${ext.guide}</a>`) : nothing}
                    ${ext.categories ? this._row('Categories', ext.categories) : nothing}
                    ${ext.keywords ? this._row('Keywords', ext.keywords) : nothing}
                    ${ext.libraries && ext.libraries.length > 0 ? this._row('Libraries', html`
                        <div class="badges">
                            ${ext.libraries.map(lib => this._libraryBadge(lib))}
                        </div>`) : nothing}
                    ${cfg.length > 0 ? this._row('Configuration', html`
                        <a class="link" @click=${this._viewConfiguration}>${cfg.join(', ')}</a>`) : nothing}

                    <div class="actions">
                        <button @click=${this._close}>Close</button>
                    </div>
                </div>
            </div>
        `;
    }

    _row(label, value) {
        return html`
            <div class="row">
                <span class="label">${label}</span>
                <span class="val">${value}</span>
            </div>`;
    }

    _libraryBadge(lib) {
        const text = `${lib.name} ${lib.version}`;
        if (lib.url) {
            return html`<span class="badge clickable"
                @click=${() => window.open(lib.url, '_blank', 'noopener,noreferrer')}>${text}</span>`;
        }
        return html`<span class="badge">${text}</span>`;
    }

    _viewConfiguration() {
        const prefixes = (this.ext.configPrefixes || []).join(',');
        const base = this.basePath || '/q/prod-ui';
        this._close();
        Router.go(base + '/configuration?filter=' + encodeURIComponent(prefixes));
    }

    _onBackdrop() {
        this._close();
    }

    _close() {
        this.dispatchEvent(new CustomEvent('close', { bubbles: true, composed: true }));
    }
}
customElements.define('pui-extension-dialog', PuiExtensionDialog);
