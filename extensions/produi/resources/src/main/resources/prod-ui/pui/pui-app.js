import { LitElement, html, css } from 'lit';
import { Router } from '@vaadin/router';
import { applyTheme } from './pui-header.js';
import { quarkusVersion } from '../produi-app-info.js';
import { pages } from '../produi-pages-data.js';

export class PuiApp extends LitElement {

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100vh;
        }
        .pui-main {
            display: flex;
            flex: 1;
            overflow: hidden;
        }
        .pui-nav {
            width: 220px;
            min-width: 220px;
            background: var(--lumo-contrast-5pct);
            border-right: 1px solid var(--lumo-contrast-10pct);
            overflow-y: auto;
            padding: 8px 0;
            display: flex;
            flex-direction: column;
        }
        .pui-content {
            flex: 1;
            overflow-y: auto;
            padding: 24px;
            background: var(--lumo-base-color);
        }
        #outlet {
            height: 100%;
        }
        #outlet > * {
            height: 100%;
        }
        .nav-link {
            display: block;
            padding: 8px 16px;
            color: var(--lumo-body-text-color);
            text-decoration: none;
            cursor: pointer;
            font-size: 14px;
            border-left: 3px solid transparent;
        }
        .nav-link:hover {
            background: var(--lumo-contrast-5pct);
        }
        .nav-link.active {
            background: var(--lumo-primary-color-10pct);
            color: var(--lumo-primary-text-color);
            border-left-color: var(--lumo-primary-color);
            font-weight: 500;
        }
        .quarkus-version {
            padding: 8px 16px;
            border-top: 1px solid var(--lumo-contrast-5pct);
            font-size: 11px;
            color: var(--lumo-contrast-40pct);
        }
    `;

    static properties = {
        _currentPath: { state: true },
        _pageTitle: { state: true },
        _subPages: { state: true },
        _activeSubPage: { state: true }
    };

    constructor() {
        super();
        this._currentPath = window.location.pathname;
        this._pageTitle = null;
        this._subPages = [];
        this._activeSubPage = null;
        applyTheme(localStorage.getItem('pui-theme') === 'dark');
    }

    firstUpdated() {
        const outlet = this.shadowRoot.getElementById('outlet');
        const path = window.location.pathname;
        const prodUiIdx = path.indexOf('/prod-ui');
        this._basePath = prodUiIdx >= 0 ? path.substring(0, prodUiIdx + '/prod-ui'.length) : '/q/prod-ui';

        // Built-in pages are contributed as internal entries in the page data and their
        // components are bundled with the shell, so each gets a direct route to its own
        // component. Driving this from the data (rather than a hard-coded list) keeps the
        // nav and routes in step with whichever built-in pages are actually enabled -
        // e.g. Diagnostics only appears when the thread dump is turned on.
        const builtInRoutes = this._builtInPages().map(p => ({
            path: this._basePath + '/' + p.slug,
            component: p.componentLink
        }));

        this._router = new Router(outlet);
        this._router.setRoutes([
            { path: this._basePath + '(/?)', component: 'pui-extensions' },
            ...builtInRoutes,
            { path: this._basePath + '/(.*)', component: 'pui-page-host' }
        ]);

        window.addEventListener('vaadin-router-location-changed', () => {
            this._currentPath = window.location.pathname;
            this._updatePageInfo();
        });

        this._updatePageInfo();
    }

    _builtInPages() {
        if (!pages) {
            return [];
        }
        return pages
            .filter(p => p.internal && p.pages && p.pages.length > 0)
            .map(p => ({
                title: p.title,
                slug: p.title.toLowerCase(),
                componentLink: p.pages[0].componentLink
            }));
    }

    _updatePageInfo() {
        const path = this._currentPath;
        const relative = path.substring(this._basePath.length);

        if (!relative || relative === '/') {
            this._pageTitle = null;
            this._subPages = [];
            this._activeSubPage = null;
            return;
        }

        // Check built-in pages (data-driven, so it also covers Advisor/Loggers/Diagnostics)
        const builtIn = this._builtInPages().find(p => relative === '/' + p.slug);
        if (builtIn) {
            this._pageTitle = builtIn.title;
            this._subPages = [];
            this._activeSubPage = null;
            return;
        }

        // Extension page - find the extension and its sub-pages
        if (pages) {
            for (const ext of pages) {
                if (ext.internal) continue;
                for (const p of ext.pages) {
                    if (relative === '/' + ext.namespace + '/' + p.componentLink) {
                        this._pageTitle = ext.title || ext.namespace;
                        this._subPages = ext.pages;
                        this._activeSubPage = p.componentLink;
                        return;
                    }
                }
            }
        }

        this._pageTitle = null;
        this._subPages = [];
        this._activeSubPage = null;
    }

    render() {
        return html`
            <pui-header
                .pageTitle=${this._pageTitle}
                .subPages=${this._subPages}
                .activeSubPage=${this._activeSubPage}
                @navigate-home=${() => this._navigate('')}
                @navigate-subpage=${(e) => this._navigateToSubPage(e.detail)}>
            </pui-header>
            <div class="pui-main">
                <nav class="pui-nav">
                    <a class="nav-link ${this._isActive('')}" @click=${() => this._navigate('')}>
                        Extensions
                    </a>
                    ${this._builtInPages().map(p => html`
                        <a class="nav-link ${this._isActive(p.slug)}" @click=${() => this._navigate(p.slug)}>
                            ${p.title}
                        </a>
                    `)}
                    <div style="flex:1"></div>
                    <div class="quarkus-version">Quarkus ${quarkusVersion}</div>
                </nav>
                <div class="pui-content">
                    <div id="outlet"></div>
                </div>
            </div>
        `;
    }

    _isActive(suffix) {
        if (!suffix) {
            return this._currentPath === this._basePath || this._currentPath === this._basePath + '/' ? 'active' : '';
        }
        return this._currentPath.endsWith('/' + suffix) ? 'active' : '';
    }

    _navigate(suffix) {
        const path = suffix ? this._basePath + '/' + suffix : this._basePath + '/';
        Router.go(path);
    }

    _navigateToSubPage(subPage) {
        // Find the extension namespace for this sub-page
        if (pages) {
            for (const ext of pages) {
                if (ext.pages && ext.pages.includes(subPage)) {
                    Router.go(this._basePath + '/' + ext.namespace + '/' + subPage.componentLink);
                    return;
                }
            }
        }
    }
}
customElements.define('pui-app', PuiApp);
