import { html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { ObservabilityCardBase } from 'observability-card-base';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-tree-toggle.js';
import '@vaadin/button';
import '@vaadin/dialog';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { dialogRenderer } from '@vaadin/dialog/lit.js';

/**
 * Dev UI Traces view: spans grouped by trace, rendered as an expandable tree with a
 * timing waterfall bar per span. Seeds from getSnapshot(), then live-updates via
 * streamSpans(). Extends ObservabilityCardBase for the shared CSV export.
 */
export class QwcOtelTraces extends ObservabilityCardBase {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host { display: flex; flex-direction: column; height: 100%; }
        .toolbar { display: flex; gap: 8px; padding: 8px; align-items: center; }
        .bar-track { position: relative; height: 14px; background: var(--lumo-contrast-5pct); border-radius: 3px; cursor: pointer; }
        .bar { position: absolute; top: 0; height: 14px; background: var(--lumo-primary-color); border-radius: 3px; }
        .span-id { color: var(--lumo-secondary-text-color); font-family: monospace; }
        .span-name { cursor: pointer; color: var(--lumo-primary-text-color); }
        .span-name:hover { text-decoration: underline; }
    `;

    static properties = {
        _traces: { state: true },
        _flatRows: { state: true },
        _detail: { state: true },
    };

    constructor() {
        super();
        this._traces = [];
        this._flatRows = [];
        this._detail = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this._load();
        this._subscribe();
    }

    disconnectedCallback() {
        this._unsubscribe();
        super.disconnectedCallback();
    }

    hotReload() {
        // Called (via QwcHotReloadElement) when the JSON-RPC connection is re-established
        // after a dev-mode live reload. The captured spans survive the reload (see the
        // dev store holder), but the previous websocket — and with it our stream
        // subscription — is gone, so re-seed from the store and re-subscribe.
        this._unsubscribe();
        this._load();
        this._subscribe();
    }

    _load() {
        this.jsonRpc.getSnapshot().then(resp => {
            this._setTraces(resp.result.traces ?? []);
        });
    }

    _subscribe() {
        this._stream = this.jsonRpc.streamSpans().onNext(() => {
            // A new span arrived; re-fetch the grouped snapshot (simple + correct for the POC).
            this._load();
        });
    }

    _unsubscribe() {
        if (this._stream) {
            this._stream.cancel();
            this._stream = null;
        }
    }

    _setTraces(traces) {
        this._traces = traces;
        // Build a tree data structure for vaadin-grid: root nodes = traces, children = spans.
        // Spans within a trace are ordered oldest-first (by start time) so the waterfall reads
        // top-to-bottom chronologically; the grouped snapshot delivers them in completion order
        // (children finish before their parents).
        this._flatRows = traces.map(t => {
            const spans = [...t.spans].sort(this._byStartAsc);
            const root = spans.find(s => this._isRoot(s, spans)) ?? spans[0];
            return {
                traceId: t.traceId,
                time: this._formatTime(t.windowStart),
                shortId: t.traceId.substring(0, 8),
                traceName: root && root.name ? root.name : '(unnamed)',
                windowStart: t.windowStart,
                windowEnd: t.windowEnd,
                children: spans.map(s => ({ ...s, _window: [t.windowStart, t.windowEnd] })),
            };
        });
    }

    _byStartAsc = (a, b) => a.startEpochNanos - b.startEpochNanos;

    /** True when a span has no parent (empty or all-zero span id — OTel's "no parent" sentinel). */
    _hasNoParent(span) {
        const p = span.parentSpanId;
        return !p || /^0+$/.test(p);
    }

    /** A span is the trace root when it has no parent, or its parent is not part of this trace. */
    _isRoot(span, spans) {
        if (this._hasNoParent(span)) {
            return true;
        }
        return !spans.some(s => s.spanId === span.parentSpanId);
    }

    /** Wall-clock time (hh:mm:ss) a trace started, from its window start (epoch nanos). */
    _formatTime(epochNanos) {
        const d = new Date(epochNanos / 1_000_000);
        const pad = (n) => String(n).padStart(2, '0');
        return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
    }

    updated() {
        // The tree grid caches dataProvider results; invalidate so live updates appear.
        this.renderRoot.querySelector('vaadin-grid')?.clearCache();
    }

    render() {
        return html`
            <div class="toolbar">
                <vaadin-button theme="small" @click=${this._clear}>Clear</vaadin-button>
                <vaadin-button theme="small" @click=${this._export}>Export CSV</vaadin-button>
                <span>${this._traces.length} traces</span>
            </div>
            <vaadin-grid .itemHasChildrenPath=${'children'}
                         .dataProvider=${this._dataProvider} theme="compact no-border">
                <vaadin-grid-column header="Name" auto-width flex-grow="3"
                    ${columnBodyRenderer((item, model) => this._nameRenderer(item, model), [])}>
                </vaadin-grid-column>
                <vaadin-grid-column header="Kind" path="kind" auto-width></vaadin-grid-column>
                <vaadin-grid-column header="Status" path="statusCode" auto-width></vaadin-grid-column>
                <vaadin-grid-column header="Duration" auto-width
                    ${columnBodyRenderer(row => row.durationNanos != null
                        ? html`${(row.durationNanos / 1_000_000).toFixed(2)} ms` : html``, [])}>
                </vaadin-grid-column>
                <vaadin-grid-column header="Timing" flex-grow="1"
                    ${columnBodyRenderer(row => this._waterfall(row), [])}>
                </vaadin-grid-column>
            </vaadin-grid>
            <vaadin-dialog .opened=${this._detail != null}
                @opened-changed=${e => { if (!e.detail.value) this._detail = null; }}
                ${dialogRenderer(() => this._renderDetail(), this._detail)}>
            </vaadin-dialog>`;
    }

    _dataProvider = (params, callback) => {
        const items = params.parentItem ? (params.parentItem.children ?? []) : this._flatRows;
        callback(items, items.length);
    };

    _nameRenderer(item, model) {
        // Trace (root) rows carry children: show "hh:mm:ss - <short trace id> - <span name (bold)>".
        // Child span rows show "<short span id> - <span name (bold, click for details)>".
        const content = item.children
            ? html`${item.time} - ${item.shortId} - <b>${item.traceName}</b>`
            : html`<span class="span-id">${item.spanId.substring(0, 8)}</span> -
                   <span class="span-name" title="Show span details"
                         @click=${(e) => this._openDetail(e, item)}><b>${item.name}</b></span>`;
        return html`
            <vaadin-grid-tree-toggle
                .leaf=${!item.children}
                .level=${model.level ?? 0}
                .expanded=${model.expanded}
                @expanded-changed=${(e) => this._onExpandedChanged(e, item)}>
                ${content}
            </vaadin-grid-tree-toggle>`;
    }

    _openDetail(e, item) {
        // Stop the click from reaching the tree-toggle (which would toggle the row).
        e.stopPropagation();
        this._detail = item;
    }

    _onExpandedChanged(e, item) {
        const grid = this.renderRoot.querySelector('vaadin-grid');
        if (!grid) {
            return;
        }
        if (e.detail.value) {
            grid.expandItem(item);
        } else {
            grid.collapseItem(item);
        }
    }

    _waterfall(row) {
        if (row.durationNanos == null || !row._window) {
            return html``;
        }
        const [start, end] = row._window;
        const total = Math.max(end - start, 1);
        const left = ((row.startEpochNanos - start) / total) * 100;
        const width = Math.max((row.durationNanos / total) * 100, 0.5);
        return html`<div class="bar-track" @click=${() => this._detail = row}>
            <div class="bar" style="left:${left}%; width:${width}%"></div>
        </div>`;
    }

    _renderDetail() {
        const r = this._detail;
        if (!r) return html``;
        return html`
            <div style="min-width: 400px;">
                <h4>${r.name}</h4>
                <p><b>traceId:</b> ${r.traceId}<br/>
                   <b>spanId:</b> ${r.spanId}<br/>
                   <b>parent:</b> ${this._hasNoParent(r) ? '(root)' : r.parentSpanId}<br/>
                   <b>kind:</b> ${r.kind}<br/>
                   <b>status:</b> ${r.statusCode} ${r.statusDescription}</p>
                <p><b>started:</b> ${this._formatClockMs(r.startEpochNanos)}<br/>
                   <b>ended:</b> ${this._formatClockMs(r.endEpochNanos)}<br/>
                   <b>duration:</b> ${(r.durationNanos / 1_000_000).toFixed(3)} ms</p>
                <b>Attributes</b>
                <ul>${Object.entries(r.attributes ?? {}).map(([k, v]) => html`<li>${k} = ${v}</li>`)}</ul>
                <b>Events</b>
                <ul>${(r.events ?? []).map(e => html`<li>${e}</li>`)}</ul>
            </div>`;
    }

    /** Full wall-clock time (hh:mm:ss.mmm) from an epoch-nanos value, for the detail drawer. */
    _formatClockMs(epochNanos) {
        const d = new Date(epochNanos / 1_000_000);
        const pad = (n, len = 2) => String(n).padStart(len, '0');
        return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}`;
    }

    _clear() {
        this.jsonRpc.clear().then(() => this._setTraces([]));
    }

    _export() {
        // Flatten every span across every trace for CSV, spans oldest-first as displayed.
        const rows = this._flatRows.flatMap(t => t.children.map(s => ({
            traceId: s.traceId, spanId: s.spanId, parentSpanId: s.parentSpanId,
            name: s.name, kind: s.kind, durationNanos: s.durationNanos,
            statusCode: s.statusCode,
        })));
        this.exportCsv(rows, 'traces.csv');
    }
}
customElements.define('qwc-otel-traces', QwcOtelTraces);
