import { LitElement, html, css } from 'lit';
import { JsonRpc } from '../controller/jsonrpc.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import '@vaadin/text-field';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';

export class PuiConfiguration extends LitElement {

    jsonRpc = new JsonRpc('quarkus-produi');

    static styles = css`
        :host { display: block; height: 100%; }
        .container { display: flex; flex-direction: column; height: 100%; }
        .search { padding: 0 0 12px; }
        .search vaadin-text-field { width: 400px; }
        .grid { flex: 1; }
        .name { font-family: monospace; font-size: 13px; color: var(--lumo-primary-text-color); }
        .value { font-family: monospace; font-size: 13px; word-break: break-all; }
        .source { font-size: 11px; color: var(--lumo-secondary-text-color); }
    `;

    static properties = {
        _configs: { state: true },
        _filtered: { state: true },
        _query: { state: true }
    };

    constructor() {
        super();
        this._query = '';
    }

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getAllConfiguration().then(response => {
            this._configs = response.result;
            this._filtered = response.result;
        });
    }

    render() {
        if (!this._configs) {
            return html`<span>Loading configuration...</span>`;
        }
        return html`
            <div class="container">
                <div class="search">
                    <vaadin-text-field
                        placeholder="Search configuration..."
                        clear-button-visible
                        @input=${this._onSearch}>
                    </vaadin-text-field>
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
        return html`<span class="value">${item.value || ''}</span>`;
    }

    _sourceRenderer(item) {
        return html`<span class="source">${item.source || ''}</span>`;
    }

    _onSearch(e) {
        const query = e.target.value?.toLowerCase() || '';
        if (!query) {
            this._filtered = this._configs;
        } else {
            this._filtered = this._configs.filter(c =>
                c.name.toLowerCase().includes(query) ||
                (c.value && c.value.toLowerCase().includes(query))
            );
        }
    }
}
customElements.define('pui-configuration', PuiConfiguration);
