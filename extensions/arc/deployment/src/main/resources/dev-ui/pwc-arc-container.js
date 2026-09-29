import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Read-only Prod UI view of the running ArC (CDI) container. It shows the
 * registered beans, observer methods and interceptors, plus the supported
 * scopes. It only displays metadata (class, scope, kind, qualifiers, types) and
 * offers no actions - no bean is created, mutated or destroyed.
 */
export class PwcArcContainer extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            padding: 10px;
            gap: 10px;
        }
        .toolbar {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .tabs {
            display: flex;
            gap: 5px;
        }
        .spacer {
            flex: 1;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .tab {
            cursor: pointer;
        }
        .tab.selected {
            color: var(--lumo-primary-text-color);
            font-weight: 600;
        }
        .grid {
            height: 100%;
        }
        .scopes {
            font-size: var(--lumo-font-size-s);
            color: var(--lumo-secondary-text-color);
        }
        code {
            font-size: 85%;
        }
        .tags {
            display: flex;
            flex-wrap: wrap;
            gap: 4px;
        }
        .tag {
            background-color: var(--lumo-contrast-5pct);
            border-radius: var(--lumo-border-radius-s);
            padding: 0 6px;
            font-size: 80%;
        }
        .yes {
            color: var(--lumo-success-text-color);
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _beans: { state: true },
        _observers: { state: true },
        _interceptors: { state: true },
        _scopes: { state: true },
        _selected: { state: true }
    };

    constructor() {
        super();
        this._selected = 'beans';
    }

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this.jsonRpc.getBeans().then(r => this._beans = r.result);
        this.jsonRpc.getObservers().then(r => this._observers = r.result);
        this.jsonRpc.getInterceptors().then(r => this._interceptors = r.result);
        this.jsonRpc.getScopes().then(r => this._scopes = r.result);
    }

    render() {
        return html`
            <div class="toolbar">
                <div class="tabs">
                    ${this._tab('beans', 'Beans', this._beans)}
                    ${this._tab('observers', 'Observers', this._observers)}
                    ${this._tab('interceptors', 'Interceptors', this._interceptors)}
                </div>
                <span class="spacer"></span>
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            ${this._renderSelected()}
            ${this._renderScopes()}`;
    }

    _tab(id, label, data) {
        const count = data ? data.length : '';
        return html`<vaadin-button theme="small tertiary" class="tab ${this._selected === id ? 'selected' : ''}"
                @click=${() => this._selected = id}>${label}${count === '' ? '' : ` (${count})`}</vaadin-button>`;
    }

    _renderSelected() {
        if (this._selected === 'observers') {
            return this._renderObservers();
        }
        if (this._selected === 'interceptors') {
            return this._renderInterceptors();
        }
        return this._renderBeans();
    }

    _renderBeans() {
        if (!this._beans) {
            return html`<span class="empty">Loading beans...</span>`;
        }
        if (this._beans.length === 0) {
            return html`<span class="empty">No beans.</span>`;
        }
        return html`
            <vaadin-grid class="grid" .items=${this._beans} theme="row-stripes" all-rows-visible>
                <vaadin-grid-sort-column path="beanClass" header="Bean" auto-width resizable
                    ${columnBodyRenderer(bean => html`<code>${bean.beanClass}</code>`, [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column path="scope" header="Scope" auto-width resizable
                    ${columnBodyRenderer(bean => html`<code>${bean.scope}</code>`, [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column path="kind" header="Kind" auto-width resizable></vaadin-grid-sort-column>
                <vaadin-grid-column header="Qualifiers" auto-width resizable
                    ${columnBodyRenderer(bean => this._renderTags(bean.qualifiers), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column header="Name" auto-width resizable
                    ${columnBodyRenderer(bean => bean.name ? html`<code>${bean.name}</code>` : html``, [])}>
                </vaadin-grid-column>
            </vaadin-grid>`;
    }

    _renderObservers() {
        if (!this._observers) {
            return html`<span class="empty">Loading observers...</span>`;
        }
        if (this._observers.length === 0) {
            return html`<span class="empty">No observers.</span>`;
        }
        return html`
            <vaadin-grid class="grid" .items=${this._observers} theme="row-stripes" all-rows-visible>
                <vaadin-grid-sort-column path="observedType" header="Observed type" auto-width resizable
                    ${columnBodyRenderer(o => html`<code>${o.observedType}</code>`, [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column path="declaringClass" header="Declaring class" auto-width resizable
                    ${columnBodyRenderer(o => html`<code>${o.declaringClass}</code>`, [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-column header="Qualifiers" auto-width resizable
                    ${columnBodyRenderer(o => this._renderTags(o.qualifiers), [])}>
                </vaadin-grid-column>
                <vaadin-grid-sort-column path="priority" header="Priority" auto-width resizable></vaadin-grid-sort-column>
                <vaadin-grid-column header="Async" auto-width resizable
                    ${columnBodyRenderer(o => o.async
                        ? html`<vaadin-icon class="yes" icon="font-awesome-solid:check"></vaadin-icon>`
                        : html``, [])}>
                </vaadin-grid-column>
                <vaadin-grid-column path="reception" header="Reception" auto-width resizable></vaadin-grid-column>
                <vaadin-grid-column path="transactionPhase" header="Transaction phase" auto-width resizable></vaadin-grid-column>
            </vaadin-grid>`;
    }

    _renderInterceptors() {
        if (!this._interceptors) {
            return html`<span class="empty">Loading interceptors...</span>`;
        }
        if (this._interceptors.length === 0) {
            return html`<span class="empty">No interceptors.</span>`;
        }
        return html`
            <vaadin-grid class="grid" .items=${this._interceptors} theme="row-stripes" all-rows-visible>
                <vaadin-grid-sort-column path="interceptorClass" header="Interceptor" auto-width resizable
                    ${columnBodyRenderer(i => html`<code>${i.interceptorClass}</code>`, [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-column header="Bindings" auto-width resizable
                    ${columnBodyRenderer(i => this._renderTags(i.bindings), [])}>
                </vaadin-grid-column>
                <vaadin-grid-sort-column path="priority" header="Priority" auto-width resizable></vaadin-grid-sort-column>
            </vaadin-grid>`;
    }

    _renderTags(tags) {
        if (!tags || tags.length === 0) {
            return html``;
        }
        return html`<div class="tags">${tags.map(t => html`<span class="tag"><code>${t}</code></span>`)}</div>`;
    }

    _renderScopes() {
        if (!this._scopes || this._scopes.length === 0) {
            return html``;
        }
        return html`<div class="scopes">Supported scopes: ${this._scopes.map(s => html`<code>${s}</code> `)}</div>`;
    }
}
customElements.define('pwc-arc-container', PwcArcContainer);
