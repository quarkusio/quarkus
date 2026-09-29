import { LitElement, html, css } from 'lit';
import { JsonRpc } from '../controller/jsonrpc.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/text-field';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import './pui-empty-state.js';

export class PuiConfiguration extends LitElement {

    jsonRpc = new JsonRpc('quarkus-produi');

    static styles = css`
        :host { display: block; height: 100%; }
        .container { display: flex; flex-direction: column; height: 100%; }
        .search { padding: 0 0 12px; display: flex; align-items: center; gap: 12px; }
        .search vaadin-text-field { width: 400px; }
        .filter-chip {
            font-size: 12px;
            font-family: monospace;
            padding: 4px 10px;
            border-radius: 12px;
            background: var(--lumo-primary-color-10pct, #e3f2fd);
            color: var(--lumo-primary-text-color, #1976d2);
            cursor: pointer;
            white-space: nowrap;
        }
        .filter-chip:hover { filter: brightness(0.96); }
        .grid { flex: 1; }
        .name { font-family: monospace; font-size: 13px; color: var(--lumo-primary-text-color); }
        .value { font-family: monospace; font-size: 13px; word-break: break-all; }
        .value.secret { color: var(--lumo-secondary-text-color); }
        .secret-tag { font-size: 10px; margin-left: 6px; color: var(--lumo-secondary-text-color); }
        .source { font-size: 11px; color: var(--lumo-secondary-text-color); }
    `;

    static properties = {
        _configs: { state: true },
        _filtered: { state: true },
        _query: { state: true },
        _error: { state: true },
        _prefixFilter: { state: true }
    };

    constructor() {
        super();
        this._query = '';
        this._error = false;
        this._prefixFilter = null;
    }

    connectedCallback() {
        super.connectedCallback();
        // Deep-link from an extension card: ?filter=<comma-separated config prefixes>.
        // Filters the (already secret-masked) config down to that extension's keys.
        const filter = new URLSearchParams(window.location.search).get('filter');
        this._prefixFilter = filter
            ? filter.split(',').map(p => p.trim()).filter(Boolean)
            : null;
        this.jsonRpc.getAllConfiguration().then(response => {
            this._configs = response.result;
            this._filtered = this._applyFilters();
        }).catch(() => {
            this._error = true;
        });
    }

    _applyFilters() {
        let result = this._configs || [];
        if (this._prefixFilter && this._prefixFilter.length > 0) {
            result = result.filter(c =>
                this._prefixFilter.some(prefix => c.name && c.name.startsWith(prefix)));
        }
        if (this._query) {
            result = result.filter(c =>
                c.name.toLowerCase().includes(this._query) ||
                (c.value && c.value.toLowerCase().includes(this._query)));
        }
        return result;
    }

    _clearPrefixFilter() {
        this._prefixFilter = null;
        this._filtered = this._applyFilters();
    }

    render() {
        if (this._error) {
            return html`<pui-empty-state kind="unavailable" heading="Configuration unavailable"
                message="The configuration could not be loaded."></pui-empty-state>`;
        }
        if (!this._configs) {
            return html`<pui-empty-state kind="loading" message="Loading configuration..."></pui-empty-state>`;
        }
        if (this._configs.length === 0) {
            return html`<pui-empty-state kind="empty" heading="No configuration"
                message="No configuration properties were reported."></pui-empty-state>`;
        }
        return html`
            <div class="container">
                <div class="search">
                    <vaadin-text-field
                        placeholder="Search configuration..."
                        clear-button-visible
                        @input=${this._onSearch}>
                    </vaadin-text-field>
                    ${this._prefixFilter && this._prefixFilter.length > 0 ? html`
                        <span class="filter-chip" @click=${this._clearPrefixFilter}
                            title="Clear filter">
                            ${this._prefixFilter.join(', ')} ✕
                        </span>
                    ` : ''}
                </div>
                <vaadin-grid .items="${this._filtered}" class="grid" theme="no-border row-stripes">
                    <vaadin-grid-sort-column auto-width header="Property" path="name"
                        ${columnBodyRenderer(this._nameRenderer, [])}>
                    </vaadin-grid-sort-column>
                    <vaadin-grid-column auto-width header="Value" flex-grow="1"
                        ${columnBodyRenderer(this._valueRenderer, [])}>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width header="Source"
                        ${columnBodyRenderer(this._sourceRenderer, [])}>
                    </vaadin-grid-column>
                </vaadin-grid>
            </div>`;
    }

    _nameRenderer(item) {
        return html`<span class="name">${item.name}</span>`;
    }

    _valueRenderer(item) {
        if (item.secret) {
            return html`<span class="value secret">${item.value || ''}</span><span class="secret-tag">🔒 hidden</span>`;
        }
        return html`<span class="value">${item.value || ''}</span>`;
    }

    _sourceRenderer(item) {
        return html`<span class="source">${item.source || ''}</span>`;
    }

    _onSearch(e) {
        this._query = e.target.value?.toLowerCase() || '';
        this._filtered = this._applyFilters();
    }
}
customElements.define('pui-configuration', PuiConfiguration);
