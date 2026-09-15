import { LitElement, html, css, nothing } from 'lit';
import { Router } from '@vaadin/router';
import '@vaadin/icon';
import { pages } from '../produi-pages-data.js';
import { getUnavailable, partitionByAvailability } from './pui-availability.js';
import './pui-empty-state.js';
import './pui-extension-link.js';
import './pui-extension-text.js';
import './pui-extension-dialog.js';

const DEFAULT_ICON = 'font-awesome-solid:puzzle-piece';

export class PuiExtensions extends LitElement {

    static properties = {
        _dialogExt: { state: true }
    };

    constructor() {
        super();
        this._dialogExt = null;
    }

    static styles = css`
        :host {
            display: block;
        }
        h2 {
            margin: 0 0 20px;
            font-size: 22px;
            font-weight: 600;
            color: var(--lumo-header-text-color, #1a1a1a);
        }
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
            gap: 16px;
        }
        .card {
            display: flex;
            flex-direction: column;
            background: var(--lumo-base-color, #fff);
            border: 1px solid var(--lumo-contrast-10pct, #e0e0e0);
            border-radius: 8px;
            padding: 20px;
            cursor: default;
            transition: box-shadow 0.15s ease;
        }
        .card:hover {
            box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        }
        .card-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 8px;
        }
        .card-icon {
            width: 22px;
            height: 22px;
            flex-shrink: 0;
            color: var(--lumo-primary-color, #1976d2);
        }
        .card-logo {
            width: 24px;
            height: 24px;
            flex-shrink: 0;
            object-fit: contain;
        }
        .card-title {
            font-size: 16px;
            font-weight: 600;
            color: var(--lumo-header-text-color, #1a1a1a);
            flex: 1;
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
        .status.experimental {
            background: #fff3e0;
            color: #e65100;
        }
        .status.preview {
            background: #ede7f6;
            color: #5e35b1;
        }
        .status.deprecated {
            background: #ffebee;
            color: #c62828;
        }
        /* Guide / config / more icons - dimmed until the card is hovered, mirroring Dev UI. */
        .hover-icon {
            width: 16px;
            height: 16px;
            flex-shrink: 0;
            cursor: pointer;
            color: var(--lumo-contrast-40pct, #999);
            opacity: 0;
            transition: opacity 0.15s ease, color 0.15s ease;
        }
        .card:hover .hover-icon {
            opacity: 1;
        }
        .hover-icon:hover {
            color: var(--lumo-primary-color, #1976d2);
        }
        .description {
            font-size: 13px;
            color: var(--lumo-secondary-text-color, #666);
            line-height: 1.4;
            margin-bottom: 12px;
        }
        .lib-badges {
            display: flex;
            flex-wrap: wrap;
            justify-content: center;
            gap: 6px;
            margin-bottom: 12px;
        }
        .lib-badge {
            font-size: 11px;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 10px;
            background: var(--lumo-contrast-5pct, #f0f0f0);
            color: var(--lumo-secondary-text-color, #666);
        }
        .lib-badge.clickable {
            cursor: pointer;
        }
        .lib-badge.clickable:hover {
            background: var(--lumo-contrast-10pct, #e6e6e6);
        }
        .divider {
            border-top: 1px solid var(--lumo-contrast-5pct, #f0f0f0);
            margin: 4px 0 8px;
        }
        .card-pages {
            display: flex;
            flex-direction: column;
            gap: 2px;
        }
        .card-texts {
            margin-top: 8px;
        }
        /* Footer mirrors Dev UI: config-filter icon (left), status pill (middle), "more" icon (right). */
        .card-footer {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 8px;
            margin-top: auto;
            padding-top: 14px;
            color: var(--lumo-contrast-40pct, #999);
        }
        .unavailable {
            margin-top: 28px;
        }
        .unavailable summary {
            cursor: pointer;
            font-size: 14px;
            font-weight: 600;
            color: var(--lumo-secondary-text-color, #666);
            padding: 8px 0;
        }
        .unavailable .grid {
            margin-top: 12px;
        }
        .card.is-unavailable {
            opacity: 0.6;
            border-style: dashed;
        }
        .card.is-unavailable:hover {
            box-shadow: none;
        }
        .card.is-unavailable .card-title {
            color: var(--lumo-secondary-text-color, #666);
        }
        .card.is-unavailable .page-name {
            font-size: 14px;
            color: var(--lumo-secondary-text-color, #666);
            padding: 4px 0;
        }
        .reason {
            margin-top: 8px;
            font-size: 12px;
            color: var(--lumo-error-text-color, #c62828);
        }
    `;

    render() {
        const extensions = pages ? pages.filter(p => !p.internal) : [];

        if (extensions.length === 0) {
            return html`
                <h2>Extensions</h2>
                <pui-empty-state
                    kind="empty"
                    heading="No extensions available"
                    message="No extensions have opted in to the production UI.">
                </pui-empty-state>
            `;
        }

        const { available, unavailable } = partitionByAvailability(extensions, getUnavailable());

        return html`
            <h2>Extensions</h2>
            ${available.length > 0 ? html`
                <div class="grid">
                    ${available.map(ext => this._renderCard(ext))}
                </div>
            ` : html`
                <pui-empty-state
                    kind="unavailable"
                    heading="No extensions currently available"
                    message="Every contributed page reported an error this session. See the group below.">
                </pui-empty-state>
            `}
            ${unavailable.length > 0 ? html`
                <details class="unavailable">
                    <summary>Unavailable (${unavailable.length})</summary>
                    <div class="grid">
                        ${unavailable.map(ext => this._renderUnavailableCard(ext))}
                    </div>
                </details>
            ` : ''}
            ${this._dialogExt ? html`
                <pui-extension-dialog
                    .ext=${this._dialogExt}
                    .basePath=${this._basePath()}
                    @close=${this._closeDialog}>
                </pui-extension-dialog>
            ` : nothing}
        `;
    }

    // The back-of-card dialog is only offered when there is something extra to show beyond the
    // front (a guide, categories/keywords, library versions or a config deep-link).
    _hasDetails(ext) {
        return !!(ext.guide || ext.categories || ext.keywords
            || (ext.libraries && ext.libraries.length > 0)
            || (ext.configPrefixes && ext.configPrefixes.length > 0));
    }

    _openDialog(ext) {
        this._dialogExt = ext;
    }

    _closeDialog() {
        this._dialogExt = null;
    }

    _basePath() {
        const path = window.location.pathname;
        const idx = path.indexOf('/prod-ui');
        return idx >= 0 ? path.substring(0, idx + '/prod-ui'.length) : '/q/prod-ui';
    }

    _renderLibBadge(lib) {
        const text = `${lib.name} ${lib.version}`;
        if (lib.url) {
            return html`<span class="lib-badge clickable"
                @click=${() => window.open(lib.url, '_blank', 'noopener,noreferrer')}>${text}</span>`;
        }
        return html`<span class="lib-badge">${text}</span>`;
    }

    _renderCard(ext) {
        const cardTexts = ext.cardTexts || [];
        return html`
            <div class="card">
                <div class="card-header">
                    ${this._renderCardEmblem(ext)}
                    <span class="card-title">${ext.title || ext.namespace}</span>
                    ${ext.guide ? html`
                        <vaadin-icon class="hover-icon" icon="font-awesome-solid:book"
                            title="Go to the ${ext.title || ext.namespace} guide"
                            @click=${() => this._openGuide(ext)}></vaadin-icon>
                    ` : nothing}
                </div>
                ${ext.description ? html`<div class="description">${ext.description}</div>` : nothing}
                ${ext.libraries && ext.libraries.length > 0 ? html`
                    <div class="lib-badges">
                        ${ext.libraries.map(lib => this._renderLibBadge(lib))}
                    </div>
                ` : nothing}
                <div class="divider"></div>
                <div class="card-pages">
                    ${ext.pages.map(page => html`
                        <pui-extension-link
                            .namespace=${ext.namespace}
                            .title=${page.title}
                            .componentLink=${page.componentLink}
                            .externalUrl=${page.externalUrl}
                            .icon=${page.icon}
                            .tooltip=${page.tooltip}
                            .staticLabel=${page.staticLabel}
                            .dynamicLabel=${page.dynamicLabel}
                            .streamingLabel=${page.streamingLabel}>
                        </pui-extension-link>
                    `)}
                </div>
                ${cardTexts.length > 0 ? html`
                    <div class="card-texts">
                        ${cardTexts.map(text => html`
                            <pui-extension-text
                                .namespace=${ext.namespace}
                                .title=${text.title}
                                .icon=${text.icon}
                                .staticText=${text.staticText}
                                .dynamicText=${text.dynamicText}
                                .streamingText=${text.streamingText}>
                            </pui-extension-text>
                        `)}
                    </div>
                ` : nothing}
                ${this._renderCardFooter(ext)}
            </div>
        `;
    }

    // Footer mirrors Dev UI's card footer: a config-filter link on the left, the status pill in the
    // middle and the "more details" ellipsis on the right. Empty <span> placeholders keep the three
    // slots aligned (space-between) when a card has no config or no status.
    _renderCardFooter(ext) {
        const hasConfig = ext.configPrefixes && ext.configPrefixes.length > 0;
        const hasDetails = this._hasDetails(ext);
        if (!hasConfig && !ext.status && !hasDetails) {
            return nothing;
        }
        return html`
            <div class="card-footer">
                ${hasConfig ? html`
                    <vaadin-icon class="hover-icon" icon="font-awesome-solid:pen-to-square"
                        title="Configuration for the ${ext.title || ext.namespace} extension"
                        @click=${() => this._viewConfiguration(ext)}></vaadin-icon>
                ` : html`<span></span>`}
                ${ext.status ? html`
                    <span class="status ${ext.status.toLowerCase()}">${ext.status}</span>
                ` : html`<span></span>`}
                ${hasDetails ? html`
                    <vaadin-icon class="hover-icon" icon="font-awesome-solid:ellipsis"
                        title="More about ${ext.title || ext.namespace}"
                        @click=${() => this._openDialog(ext)}></vaadin-icon>
                ` : html`<span></span>`}
            </div>
        `;
    }

    _openGuide(ext) {
        if (ext.guide) {
            window.open(ext.guide, '_blank', 'noopener,noreferrer');
        }
    }

    _viewConfiguration(ext) {
        const prefixes = (ext.configPrefixes || []).join(',');
        Router.go(this._basePath() + '/configuration?filter=' + encodeURIComponent(prefixes));
    }

    // Prefer a real extension logo (from quarkus-extension.yaml metadata.icon-url) when present, and
    // fall back to a font-awesome icon otherwise. If the logo fails to load, swap in the icon fallback.
    _renderCardEmblem(ext) {
        if (ext.logo) {
            return html`
                <img class="card-logo" src=${ext.logo} alt=""
                     @error=${e => this._onLogoError(e, ext)}>
            `;
        }
        return html`<vaadin-icon class="card-icon" icon=${this._cardIcon(ext)}></vaadin-icon>`;
    }

    _onLogoError(e, ext) {
        // Drop the broken image and show the icon fallback instead.
        ext.logo = null;
        this.requestUpdate();
    }

    _cardIcon(ext) {
        const withIcon = (ext.pages || []).find(p => p.icon);
        return withIcon ? withIcon.icon : DEFAULT_ICON;
    }

    _renderUnavailableCard(ext) {
        return html`
            <div class="card is-unavailable">
                <div class="card-title">${ext.title || ext.namespace}</div>
                <div class="card-pages">
                    ${ext.pages.map(page => html`
                        <span class="page-name">${page.title}</span>
                    `)}
                </div>
                <div class="reason">${ext.unavailableReason || 'Unavailable'}</div>
            </div>
        `;
    }
}
customElements.define('pui-extensions', PuiExtensions);
