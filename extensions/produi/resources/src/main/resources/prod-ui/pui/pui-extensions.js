import { LitElement, html, css } from 'lit';
import { Router } from '@vaadin/router';
import { pages } from '../produi-pages-data.js';

export class PuiExtensions extends LitElement {

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
        .card-title {
            font-size: 16px;
            font-weight: 600;
            color: var(--lumo-header-text-color, #1a1a1a);
            margin-bottom: 12px;
            padding-bottom: 8px;
            border-bottom: 1px solid var(--lumo-contrast-5pct, #f0f0f0);
        }
        .card-pages {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }
        .page-link {
            font-size: 14px;
            color: var(--lumo-primary-text-color, #1976d2);
            cursor: pointer;
            padding: 4px 0;
            text-decoration: none;
        }
        .page-link:hover {
            text-decoration: underline;
        }
        .empty {
            color: var(--lumo-secondary-text-color, #666);
            font-style: italic;
        }
    `;

    render() {
        const extensions = pages ? pages.filter(p => !p.internal) : [];

        if (extensions.length === 0) {
            return html`
                <h2>Extensions</h2>
                <p class="empty">No extensions have opted in to the production UI.</p>
            `;
        }

        return html`
            <h2>Extensions</h2>
            <div class="grid">
                ${extensions.map(ext => html`
                    <div class="card">
                        <div class="card-title">${ext.title || ext.namespace}</div>
                        <div class="card-pages">
                            ${ext.pages.map(page => html`
                                <span class="page-link"
                                      @click=${() => this._navigate(ext.namespace, page.componentLink)}>
                                    ${page.title}
                                </span>
                            `)}
                        </div>
                    </div>
                `)}
            </div>
        `;
    }

    _navigate(namespace, componentLink) {
        const path = window.location.pathname;
        const prodUiIdx = path.indexOf('/prod-ui');
        const basePath = prodUiIdx >= 0 ? path.substring(0, prodUiIdx + '/prod-ui'.length) : '/q/prod-ui';
        Router.go(basePath + '/' + namespace + '/' + componentLink);
    }
}
customElements.define('pui-extensions', PuiExtensions);
