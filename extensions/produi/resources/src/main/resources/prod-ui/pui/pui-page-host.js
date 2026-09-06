import { LitElement, html, css } from 'lit';
import { Router } from '@vaadin/router';
import { markUnavailable } from './pui-availability.js';
import './pui-empty-state.js';

export class PuiPageHost extends LitElement {

    static styles = css`
        :host {
            display: block;
            width: 100%;
            height: 100%;
        }
        .back {
            font-size: 14px;
            color: var(--lumo-primary-text-color, #1976d2);
            cursor: pointer;
            text-decoration: none;
        }
        .back:hover {
            text-decoration: underline;
        }
    `;

    static properties = {
        _component: { state: true },
        _loading: { state: true },
        _failed: { state: true }
    };

    constructor() {
        super();
        this._component = null;
        this._loading = true;
        this._failed = false;
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
        this._failed = false;
        try {
            // The bundled component is at bundle/<componentLink>
            const path = window.location.pathname;
            const prodUiIdx = path.indexOf('/prod-ui');
            const basePath = prodUiIdx >= 0 ? path.substring(0, prodUiIdx + '/prod-ui'.length) : '/q/prod-ui';
            const bundlePath = basePath + '/bundle/' + this._componentLink;
            await import(bundlePath);
        } catch (e) {
            console.warn('Could not load component bundle:', this._componentLink, e);
            // Remember the failure so the Extensions landing can list this page in
            // its collapsed "Unavailable" group instead of the page just vanishing.
            this._failed = true;
            markUnavailable(this._namespace, 'This page could not be loaded.');
        }
        this._loading = false;
    }

    render() {
        if (this._loading) {
            return html`<pui-empty-state kind="loading" message="Loading page..."></pui-empty-state>`;
        }
        if (this._failed || !this._component) {
            return html`
                <pui-empty-state
                    kind="unavailable"
                    heading="Page unavailable"
                    message="This page could not be loaded. It may be temporarily unavailable.">
                    <a class="back" @click=${this._goHome}>Back to Extensions</a>
                </pui-empty-state>
            `;
        }
        const el = document.createElement(this._component);
        el.setAttribute('namespace', this._namespace);
        return html`${el}`;
    }

    _goHome() {
        const path = window.location.pathname;
        const prodUiIdx = path.indexOf('/prod-ui');
        const basePath = prodUiIdx >= 0 ? path.substring(0, prodUiIdx + '/prod-ui'.length) : '/q/prod-ui';
        Router.go(basePath + '/');
    }
}
customElements.define('pui-page-host', PuiPageHost);
