import { LitElement, html, css } from 'lit';
import { JsonRpc } from '../controller/jsonrpc.js';
import '@vaadin/icon';
import '@vaadin/button';
import './pui-empty-state.js';

/**
 * Read-only Prod UI production-readiness / security advisor. It calls the read-only
 * getReadinessChecks JSON-RPC method and renders an overall score plus each check
 * with a PASS / WARN / FAIL status. It only displays findings - it never offers a
 * control to change anything, and check details never contain secret values (secrets
 * are reported by name only, server-side).
 */
export class PuiAdvisor extends LitElement {

    jsonRpc = new JsonRpc('quarkus-produi');

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            gap: 16px;
        }
        .toolbar {
            display: flex;
            gap: 10px;
            align-items: center;
        }
        .toolbar .spacer { flex: 1; }
        .button {
            background-color: transparent;
            cursor: pointer;
        }
        .score {
            display: flex;
            align-items: baseline;
            gap: 10px;
        }
        .score .number {
            font-size: 42px;
            font-weight: 700;
        }
        .score .label {
            color: var(--lumo-secondary-text-color);
        }
        .score.good .number { color: var(--lumo-success-text-color); }
        .score.warn .number { color: var(--lumo-warning-text-color, #b26a00); }
        .score.bad .number { color: var(--lumo-error-text-color); }
        .checks {
            display: flex;
            flex-direction: column;
            gap: 10px;
        }
        .check {
            display: flex;
            gap: 12px;
            align-items: flex-start;
            padding: 12px 14px;
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: var(--lumo-border-radius-m);
        }
        .check .body { flex: 1; }
        .check .title { font-weight: 600; }
        .check .detail {
            color: var(--lumo-secondary-text-color);
            font-size: 14px;
            margin-top: 2px;
        }
        .check .category {
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.04em;
            color: var(--lumo-tertiary-text-color, var(--lumo-secondary-text-color));
        }
        .badge {
            font-size: 11px;
            font-weight: 700;
            padding: 2px 8px;
            border-radius: 10px;
            white-space: nowrap;
        }
        .badge.PASS { background: var(--lumo-success-color); color: white; }
        .badge.WARN { background: var(--lumo-warning-color, #b26a00); color: white; }
        .badge.FAIL { background: var(--lumo-error-color); color: white; }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _score: { state: true },
        _checks: { state: true },
        _error: { state: true }
    };

    constructor() {
        super();
        this._error = false;
    }

    connectedCallback() {
        super.connectedCallback();
        this._load();
    }

    _load() {
        this._checks = undefined;
        this._error = false;
        this.jsonRpc.getReadinessChecks().then(response => {
            const result = response.result || {};
            this._score = result.score;
            this._checks = result.checks || [];
        }).catch(() => {
            this._error = true;
        });
    }

    _scoreClass() {
        if (this._score >= 80) {
            return 'good';
        }
        if (this._score >= 50) {
            return 'warn';
        }
        return 'bad';
    }

    render() {
        if (this._error) {
            return html`<pui-empty-state kind="unavailable" heading="Advisor unavailable"
                message="The readiness checks could not be run."></pui-empty-state>`;
        }
        if (!this._checks) {
            return html`<pui-empty-state kind="loading" message="Running readiness checks..."></pui-empty-state>`;
        }
        if (this._checks.length === 0) {
            return html`<pui-empty-state kind="empty" heading="No checks"
                message="No readiness checks were reported."></pui-empty-state>`;
        }
        return html`
            <div class="toolbar">
                <div class="score ${this._scoreClass()}">
                    <span class="number">${this._score}</span>
                    <span class="label">/ 100 readiness</span>
                </div>
                <div class="spacer"></div>
                <vaadin-button theme="small" @click=${this._load} class="button">
                    <vaadin-icon icon="lumo:reload"></vaadin-icon> Refresh
                </vaadin-button>
            </div>
            <div class="checks">
                ${this._checks.map(c => this._renderCheck(c))}
            </div>`;
    }

    _renderCheck(check) {
        return html`
            <div class="check">
                <span class="badge ${check.status}">${check.status}</span>
                <div class="body">
                    <div class="category">${check.category}</div>
                    <div class="title">${check.title}</div>
                    <div class="detail">${check.detail}</div>
                </div>
            </div>`;
    }
}
customElements.define('pui-advisor', PuiAdvisor);
