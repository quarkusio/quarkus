import { html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { ObservabilityCardBase } from 'observability-card-base';
import 'echarts-line';
import '@vaadin/button';

/**
 * Unified Dev UI metrics page. Left panel: a grouped, checkbox catalog picker (only the
 * selected metrics are captured/charted). Right panel: one time-series line chart section
 * per selected metric name, one line per tag combination. Cumulative series are rendered as
 * a per-interval rate. Rendering and the live stream are held only while the page is the
 * visible/focused view. Extends ObservabilityCardBase for CSV export + hot-reload.
 */
export class QwcMetrics extends ObservabilityCardBase {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host { display: flex; height: 100%; gap: 15px; padding: 15px; }
        .picker { width: 280px; overflow: auto; border-right: 1px solid var(--lumo-contrast-10pct); padding-right: 10px; }
        .charts { flex: 1; display: flex; flex-direction: column; gap: 20px; overflow: auto; }
        .group-title { font-weight: bold; margin: 10px 0 4px; color: var(--lumo-secondary-text-color); }
        .metric { display: flex; align-items: center; gap: 6px; padding: 2px 0; }
        /* flex-shrink:0 so stacked charts keep their height and .charts scrolls, instead of the
           column flexbox collapsing every section (and its chart) to near-zero once they overflow. */
        .section { display: flex; flex-direction: column; height: 320px; flex-shrink: 0; max-width: 960px; }
        .section h4 { margin: 0 0 4px; }
        .empty { color: var(--lumo-secondary-text-color); padding: 20px; }
        .toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 10px; }
        echarts-line { flex: 1; }
    `;

    static properties = {
        _catalog: { state: true },
        _selection: { state: true },
        _sections: { state: true }, // name -> Map<seriesKey, {tags, source, cumulative, type, points:[[ts,val]]}>
    };

    constructor() {
        super();
        this._catalog = { groups: [] };
        this._selection = new Set();
        this._sections = {};
        this._retentionMillis = 600000; // overwritten from getCatalog()
        this._visHandler = () => this._onVisibilityChange();
    }

    connectedCallback() {
        super.connectedCallback();
        document.addEventListener('visibilitychange', this._visHandler);
        this._loadCatalog();
        if (!document.hidden) {
            this._load();
            this._subscribe();
        }
        this._catalogTimer = setInterval(() => this._loadCatalog(), 5000);
    }

    disconnectedCallback() {
        document.removeEventListener('visibilitychange', this._visHandler);
        clearInterval(this._catalogTimer);
        this._unsubscribe();
        super.disconnectedCallback();
    }

    hotReload() {
        this._unsubscribe();
        this._loadCatalog();
        this._load();
        if (!document.hidden) {
            this._subscribe();
        }
    }

    _onVisibilityChange() {
        if (document.hidden) {
            this._unsubscribe(); // stop stream + redraws in background
        } else {
            this._load();        // re-seed from the (preserved) store
            this._subscribe();
        }
    }

    _loadCatalog() {
        this.jsonRpc.getCatalog().then(resp => {
            const result = resp.result ?? { groups: [] };
            this._catalog = result;
            if (result.retentionMillis) {
                this._retentionMillis = result.retentionMillis;
            }
        });
    }

    // Series identity must include source: the same name+tags can arrive from both the
    // Micrometer and OTel backends and must render as two distinct lines.
    _seriesKey(s) {
        return (s.source ?? '') + '|' + JSON.stringify(s.tags);
    }

    _load() {
        this.jsonRpc.getSnapshot().then(resp => {
            const sections = {};
            (resp.result?.sections ?? []).forEach(section => {
                const byKey = {};
                section.series.forEach(s => {
                    byKey[this._seriesKey(s)] = s;
                });
                sections[section.name] = byKey;
            });
            this._sections = sections;
        });
    }

    _subscribe() {
        this._stream = this.jsonRpc.streamMetrics().onNext(msg => {
            const s = msg.result;
            if (!s || !this._selection.has(s.name)) {
                return;
            }
            const section = { ...(this._sections[s.name] ?? {}) };
            const key = this._seriesKey(s);
            const existing = section[key]
                ?? { tags: s.tags, source: s.source, cumulative: s.cumulative, type: s.type, points: [] };
            // Trim to the server's retention window (from getCatalog) so the client never over-retains.
            const cutoff = s.timestamp - this._retentionMillis;
            existing.points = [...existing.points, [s.timestamp, s.value]].filter(p => p[0] >= cutoff);
            section[key] = existing;
            this._sections = { ...this._sections, [s.name]: section };
        });
    }

    _unsubscribe() {
        if (this._stream) {
            this._stream.cancel();
            this._stream = null;
        }
    }

    _toggle(name, checked) {
        const sel = new Set(this._selection);
        if (checked) {
            sel.add(name);
        } else {
            sel.delete(name);
            const copy = { ...this._sections };
            delete copy[name];
            this._sections = copy;
        }
        this._selection = sel;
        this.jsonRpc.setSelection({ names: [...sel] }).then(() => {
            this._load();
        });
    }

    // Build echarts-line `series` JSON for a section: one line per tag combo, cumulative -> rate.
    _seriesFor(name) {
        const byKey = this._sections[name] ?? {};
        return JSON.stringify(Object.values(byKey).map(s => ({
            name: this._legend(s),
            data: s.cumulative ? this._toRate(s.points) : s.points,
        })));
    }

    _legend(s) {
        const parts = Object.entries(s.tags ?? {}).map(([k, v]) => `${k}=${v}`);
        const label = parts.length ? parts.join(', ') : '(no tags)';
        // Badge the source so two lines with identical tags from different backends are distinguishable.
        return s.source ? `${label} [${s.source}]` : label;
    }

    _toRate(points) {
        const out = [];
        for (let i = 1; i < points.length; i++) {
            const [t0, v0] = points[i - 1];
            const [t1, v1] = points[i];
            const dt = (t1 - t0) / 1000;
            let dv = v1 - v0;
            if (dv < 0) {
                dv = 0; // counter reset
            }
            out.push([t1, dt > 0 ? dv / dt : 0]);
        }
        return out;
    }

    _clear() {
        this.jsonRpc.clear().then(() => {
            // The server drops history, catalog AND selection — mirror that here so the picker
            // checkboxes and charts don't desync from the now-empty server-side selection.
            this._selection = new Set();
            this._sections = {};
            this._loadCatalog();
            this._load();
        });
    }

    _export() {
        const rows = [];
        Object.entries(this._sections).forEach(([name, byKey]) => {
            Object.values(byKey).forEach(s => {
                s.points.forEach(([ts, val]) => {
                    rows.push({ name, series: this._legend(s), timestamp: ts, value: val });
                });
            });
        });
        this.exportCsv(rows, 'metrics.csv');
    }

    render() {
        const selectedNames = [...this._selection];
        return html`
            <div class="picker">
                ${(this._catalog.groups ?? []).map(g => html`
                    <div class="group-title">${g.group}</div>
                    ${g.metrics.map(m => html`
                        <label class="metric">
                            <input type="checkbox"
                                   .checked=${this._selection.has(m.name)}
                                   @change=${e => this._toggle(m.name, e.target.checked)}>
                            ${m.name} <small>(${m.type.toLowerCase()})</small>
                        </label>`)}
                `)}
            </div>
            <div class="charts">
                <div class="toolbar">
                    <vaadin-button theme="small" @click=${this._clear}>Clear</vaadin-button>
                    <vaadin-button theme="small" @click=${this._export}>Export CSV</vaadin-button>
                    <span>${selectedNames.length} selected</span>
                </div>
                ${selectedNames.length === 0
                    ? html`<div class="empty">Select metrics on the left to start charting them.</div>`
                    : selectedNames.map(name => html`
                        <div class="section">
                            <h4>${name}</h4>
                            <echarts-line series=${this._seriesFor(name)}></echarts-line>
                        </div>`)}
            </div>`;
    }
}
customElements.define('qwc-metrics', QwcMetrics);
