import { LitElement, html, css } from 'lit';
import { JsonRpc } from '../controller/jsonrpc.js';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import '@vaadin/text-field';
import '@vaadin/icon';
import '@vaadin/button';
import './pui-empty-state.js';

/**
 * Read-only Prod UI view of the application's loggers and their levels. This is
 * the production-safe counterpart of the Dev UI log viewer: it lists each logger
 * with its configured and effective level but offers no control to change a
 * level (that action is Dev UI only), so nothing is mutated.
 */
export class PuiLoggers extends LitElement {

    jsonRpc = new JsonRpc('quarkus-produi');

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            gap: 10px;
        }
        .toolbar {
            display: flex;
            gap: 10px;
            align-items: center;
        }
        .filter {
            flex: 1;
        }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .grid {
            height: 100%;
        }
        .name {
            font-family: monospace;
            font-size: 13px;
        }
        .level {
            font-weight: 600;
        }
        .OFF, .SEVERE, .ERROR, .FATAL { color: var(--lumo-error-text-color); }
        .WARN, .WARNING { color: var(--lumo-warning-text-color, #b26a00); }
        .INFO { color: var(--lumo-success-text-color); }
        .DEBUG, .FINE, .FINER, .TRACE, .FINEST, .ALL {
            color: var(--lumo-secondary-text-color);
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _loggers: { state: true },
        _filter: { state: true },
        _error: { state: true }
    };

    connectedCallback() {
        super.connectedCallback();
        this._filter = '';
        this._error = false;
        this._load();
    }

    _load() {
        this._loggers = undefined;
        this._error = false;
        this.jsonRpc.getLoggers().then(response => {
            this._loggers = (response.result || []).slice().sort((a, b) => a.name.localeCompare(b.name));
        }).catch(() => {
            this._error = true;
        });
    }

    _filtered() {
        if (!this._filter) {
            return this._loggers;
        }
        const q = this._filter.toLowerCase();
        return this._loggers.filter(l => l.name.toLowerCase().includes(q));
    }

    render() {
        if (this._error) {
            return html`<pui-empty-state kind="unavailable" heading="Loggers unavailable"
                message="The logger list could not be loaded."></pui-empty-state>`;
        }
        if (!this._loggers) {
            return html`<pui-empty-state kind="loading" message="Loading loggers..."></pui-empty-state>`;
        }
        if (this._loggers.length === 0) {
            return html`<pui-empty-state kind="empty" heading="No loggers"
                message="No loggers were reported."></pui-empty-state>`;
        }
        return html`
            <div class="toolbar">
                <vaadin-text-field class="filter" placeholder="Filter by name" clear-button-visible
                    @value-changed=${e => this._filter = e.detail.value}>
                    <vaadin-icon slot="prefix" icon="font-awesome-solid:filter"></vaadin-icon>
                </vaadin-text-field>
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <vaadin-grid .items=${this._filtered()} class="grid" theme="no-border row-stripes" all-rows-visible>
                <vaadin-grid-sort-column auto-width resizable header="Name" path="name"
                    ${columnBodyRenderer(l => html`<span class="name">${l.name}</span>`, [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width resizable header="Configured level" path="configuredLevel"
                    ${columnBodyRenderer(l => this._renderLevel(l.configuredLevel), [])}>
                </vaadin-grid-sort-column>
                <vaadin-grid-sort-column auto-width resizable header="Effective level" path="effectiveLevel"
                    ${columnBodyRenderer(l => this._renderLevel(l.effectiveLevel), [])}>
                </vaadin-grid-sort-column>
            </vaadin-grid>`;
    }

    _renderLevel(level) {
        if (!level) {
            return html`<span class="level">-</span>`;
        }
        return html`<span class="level ${level}">${level}</span>`;
    }
}
customElements.define('pui-loggers', PuiLoggers);
