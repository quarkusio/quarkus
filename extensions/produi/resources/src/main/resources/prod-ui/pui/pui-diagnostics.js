import { LitElement, html, css } from 'lit';
import { JsonRpc } from '../controller/jsonrpc.js';
import '@vaadin/icon';
import '@vaadin/button';

/**
 * Gated, read-only diagnostics page. It offers a single action - capture a thread dump - which is only reachable when
 * the operator has enabled it (quarkus.prod-ui.diagnostics.thread-dump=true) and is already behind whatever secures
 * Prod UI. A thread dump reads stack traces only: no heap dump, no file written, and no variable/field values, so it
 * exposes no secrets. The action asks for confirmation before running and each capture is audit-logged server-side.
 */
export class PuiDiagnostics extends LitElement {

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
        .note {
            color: var(--lumo-secondary-text-color);
            font-size: 14px;
        }
        .meta {
            color: var(--lumo-secondary-text-color);
            font-size: 13px;
        }
        .dump {
            flex: 1;
            overflow: auto;
            margin: 0;
            padding: 12px 14px;
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: var(--lumo-border-radius-m);
            background: var(--lumo-contrast-5pct);
            font-family: var(--lumo-font-family-monospace, monospace);
            font-size: 12px;
            white-space: pre;
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _dump: { state: true },
        _threadCount: { state: true },
        _loading: { state: true }
    };

    _capture() {
        // Read-only, but it is the only action Prod UI offers - confirm before running.
        if (!window.confirm('Capture a thread dump? This reads stack traces only (no heap, no file) and is logged.')) {
            return;
        }
        this._loading = true;
        this.jsonRpc.threadDump().then(response => {
            const result = response.result || {};
            this._loading = false;
            if (result.enabled === false) {
                this._dump = result.message || 'Thread dump is disabled.';
                this._threadCount = undefined;
                return;
            }
            this._dump = result.dump || '';
            this._threadCount = result.threadCount;
        }).catch(() => {
            this._loading = false;
            this._dump = 'Thread dump failed.';
            this._threadCount = undefined;
        });
    }

    render() {
        return html`
            <div class="toolbar">
                <vaadin-button theme="small primary" @click=${this._capture} class="button" ?disabled=${this._loading}>
                    <vaadin-icon icon="lumo:play"></vaadin-icon> Capture thread dump
                </vaadin-button>
                ${this._threadCount !== undefined
                    ? html`<span class="meta">${this._threadCount} threads</span>`
                    : ''}
                <div class="spacer"></div>
                <span class="note">Read-only: stack traces only, no heap dump, no secrets.</span>
            </div>
            ${this._renderDump()}`;
    }

    _renderDump() {
        if (this._loading) {
            return html`<span class="empty">Capturing thread dump...</span>`;
        }
        if (this._dump === undefined) {
            return html`<span class="empty">No thread dump captured yet.</span>`;
        }
        return html`<pre class="dump">${this._dump}</pre>`;
    }
}
customElements.define('pui-diagnostics', PuiDiagnostics);
