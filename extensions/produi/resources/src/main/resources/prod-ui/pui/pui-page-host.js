import { LitElement, html, css } from 'lit';

export class PuiPageHost extends LitElement {

    static styles = css`
        :host {
            display: block;
            width: 100%;
            height: 100%;
        }
    `;

    static properties = {
        _component: { state: true },
        _loading: { state: true }
    };

    constructor() {
        super();
        this._component = null;
        this._loading = true;
    }

    onBeforeEnter(location) {
        const path = location.pathname;
        const parts = path.split('/').filter(p => p.length > 0);
        if (parts.length >= 2) {
            this._namespace = parts[parts.length - 2];
            this._componentLink = parts[parts.length - 1];
            this._component = this._componentLink.replace('.js', '');
            this._loadComponent();
        }
    }

    async _loadComponent() {
        this._loading = true;
        try {
            // The bundled component is at bundle/<componentLink>
            const path = window.location.pathname;
            const prodUiIdx = path.indexOf('/prod-ui');
            const basePath = prodUiIdx >= 0 ? path.substring(0, prodUiIdx + '/prod-ui'.length) : '/q/prod-ui';
            const bundlePath = basePath + '/bundle/' + this._componentLink;
            await import(bundlePath);
        } catch (e) {
            console.warn('Could not load component bundle:', this._componentLink, e);
        }
        this._loading = false;
    }

    render() {
        if (this._loading) {
            return html`<span>Loading...</span>`;
        }
        if (!this._component) {
            return html`<p>Page not found</p>`;
        }
        const el = document.createElement(this._component);
        el.setAttribute('namespace', this._namespace);
        return html`${el}`;
    }
}
customElements.define('pui-page-host', PuiPageHost);
