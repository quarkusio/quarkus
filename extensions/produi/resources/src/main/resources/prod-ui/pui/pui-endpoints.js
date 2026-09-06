import { LitElement, html, css } from 'lit';
import { JsonRpc } from '../controller/jsonrpc.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import './pui-empty-state.js';

export class PuiEndpoints extends LitElement {

    jsonRpc = new JsonRpc('quarkus-produi');

    static styles = css`
        :host { display: block; height: 100%; }
        .grid { height: 100%; }
        .path { font-family: monospace; font-size: 13px; }
        a.path {
            color: var(--lumo-primary-text-color);
            text-decoration: none;
        }
        a.path:hover {
            text-decoration: underline;
        }
        .methods { font-size: 12px; color: var(--lumo-secondary-text-color); }
    `;

    static properties = {
        _routes: { state: true },
        _httpInfo: { state: true },
        _error: { state: true }
    };

    constructor() {
        super();
        this._error = false;
        this._httpInfo = {};
    }

    connectedCallback() {
        super.connectedCallback();
        // Load the HTTP interface info and the routes together. The info tells us which port/root-path the
        // application is served on so we can build links that target it rather than the management interface
        // this UI is served from. If the info call fails we still show the routes, just without absolute links.
        Promise.all([
            this.jsonRpc.getHttpInfo().then(response => response.result).catch(() => ({})),
            this.jsonRpc.getAllRoutes().then(response => response.result)
        ]).then(([info, routes]) => {
            this._httpInfo = info || {};
            this._routes = routes;
        }).catch(() => {
            this._error = true;
        });
    }

    render() {
        if (this._error) {
            return html`<pui-empty-state kind="unavailable" heading="Endpoints unavailable"
                message="The endpoint list could not be loaded."></pui-empty-state>`;
        }
        if (!this._routes) {
            return html`<pui-empty-state kind="loading" message="Loading endpoints..."></pui-empty-state>`;
        }
        if (this._routes.length === 0) {
            return html`<pui-empty-state kind="empty" heading="No endpoints"
                message="No HTTP endpoints were reported."></pui-empty-state>`;
        }
        return html`
            <vaadin-grid .items="${this._routes}" class="grid" theme="no-border row-stripes">
                <vaadin-grid-sort-column auto-width header="Path" path="path"
                    .renderer="${this._pathRenderer}"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width header="Methods" path="methods"></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }

    // Native vaadin-grid cell renderer (columnBodyRenderer is only a Lit wrapper around this, and its module is
    // not resolvable in the Prod UI bundle). Mirrors Dev UI's qwc-endpoints: only GET routes become a clickable
    // link (GET is safe/read-only, so opening one in a new tab cannot mutate state). Non-GET routes stay plain
    // text - Prod UI deliberately does not offer a one-click way to invoke a mutating endpoint.
    _pathRenderer = (root, column, model) => {
        const route = model.item;
        const isGet = route.methods && route.methods.split(/\s+/).includes('GET');
        root.textContent = '';
        if (isGet) {
            const a = document.createElement('a');
            a.className = 'path';
            a.href = this._href(route.path);
            a.target = '_blank';
            a.rel = 'noopener noreferrer';
            a.textContent = route.path;
            root.appendChild(a);
        } else {
            const span = document.createElement('span');
            span.className = 'path';
            span.textContent = route.path;
            root.appendChild(span);
        }
    };

    // Build a link to the application interface. The UI runs on the management interface, so we combine the
    // browser's current host/protocol with the application's own port and root path.
    _href(path) {
        const info = this._httpInfo || {};
        const full = this._joinRoot(info.rootPath, path);
        if (info.port != null) {
            return `${window.location.protocol}//${window.location.hostname}:${info.port}${full}`;
        }
        return full;
    }

    _joinRoot(rootPath, path) {
        let root = rootPath || '/';
        if (root.endsWith('/')) {
            root = root.slice(0, -1);
        }
        return root + (path.startsWith('/') ? path : '/' + path);
    }
}
customElements.define('pui-endpoints', PuiEndpoints);
