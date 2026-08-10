import { LitElement, html, css, nothing } from 'lit';
import { JsonRpc } from 'jsonrpc';
import { StorageController } from 'storage-controller';
import '@vaadin/icon';
import 'pui-echart-bar';
import 'pui-echart-gauge';

/**
 * Read-only Prod UI metrics graphs. Every registered meter is charted here (this page is not a
 * subset of the meters - the "Raw data" page holds the same meters in a searchable table). Meters
 * are organised into a section per meter type (gauges, counters, timers, ...). Within a section,
 * a meter family that splits into several tagged series (memory pools, HTTP URIs, thread states,
 * ...) becomes a horizontal bar chart, and a meter with a single value becomes a radial gauge.
 * Charts use the statistic that matters for the type (gauge value, counter count, timer/summary
 * max, long-task active count). Values stream live over JSON-RPC.
 *
 * Each chart can be expanded to a fullscreen overlay or hidden; hidden charts are remembered in
 * localStorage (a purely client-side view preference - nothing about the meters is mutated).
 * Reading meters is non-destructive - no configuration or mutation.
 */
export class PwcMicrometerMetrics extends LitElement {

    jsonRpc = new JsonRpc(this);
    storage = new StorageController(this);

    // A meter family becomes a bar chart once it has at least this many tagged series; a family
    // with a single value becomes a gauge instead.
    static MIN_SERIES = 2;
    // Cap the bars per chart so a very wide family stays readable (the family still appears).
    static MAX_BARS = 15;
    // localStorage key (under the StorageController's per-component prefix) for the hidden set.
    static HIDDEN_KEY = 'hidden';

    // The type sections in display order, each with the statistic that best represents that
    // meter type and a human label. Types not listed here still get a section (see _buildSections).
    static TYPE_SECTIONS = [
        { type: 'GAUGE', label: 'Gauges', stat: 'VALUE' },
        { type: 'COUNTER', label: 'Counters', stat: 'COUNT' },
        { type: 'TIMER', label: 'Timers (max)', stat: 'MAX' },
        { type: 'LONG_TASK_TIMER', label: 'Long task timers (active)', stat: 'ACTIVE_TASKS' },
        { type: 'DISTRIBUTION_SUMMARY', label: 'Distribution summaries (max)', stat: 'MAX' }
    ];

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            height: 100%;
            overflow: auto;
        }
        .toolbar {
            display: flex;
            justify-content: flex-end;
            align-items: center;
            gap: 12px;
            padding: 5px 10px;
        }
        .live {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            font-size: var(--lumo-font-size-s);
            color: var(--lumo-secondary-text-color);
        }
        .live .dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: var(--lumo-success-color);
            animation: pui-pulse 2s ease-in-out infinite;
        }
        @keyframes pui-pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.3; }
        }
        .hidden-btn {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            font-size: var(--lumo-font-size-s);
            padding: 4px 10px;
            border-radius: var(--lumo-border-radius-m);
            border: 1px solid var(--lumo-contrast-20pct);
            background: var(--lumo-base-color);
            color: var(--lumo-secondary-text-color);
            cursor: pointer;
        }
        .hidden-btn:hover {
            color: var(--lumo-body-text-color);
        }
        .hidden-btn vaadin-icon {
            width: 14px;
            height: 14px;
        }
        .section-heading {
            padding: 12px 10px 4px;
            font-size: var(--lumo-font-size-l);
            font-weight: 600;
            color: var(--lumo-body-text-color);
        }
        .section-heading .count {
            font-size: var(--lumo-font-size-s);
            font-weight: 400;
            color: var(--lumo-secondary-text-color);
            margin-left: 6px;
        }
        .charts {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
            gap: 12px;
            padding: 0 10px 8px;
        }
        .gauges {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
            gap: 12px;
            padding: 0 10px 16px;
        }
        .card {
            position: relative;
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: var(--lumo-border-radius-m);
            padding: 8px;
        }
        .gauge {
            height: 200px;
        }
        .card-tools {
            position: absolute;
            top: 6px;
            right: 6px;
            display: flex;
            gap: 6px;
            opacity: 0;
            transition: opacity 0.15s ease-in-out;
            z-index: 1;
        }
        .card:hover .card-tools {
            opacity: 1;
        }
        .tool {
            width: 16px;
            height: 16px;
            padding: 3px;
            border-radius: var(--lumo-border-radius-s);
            color: var(--lumo-secondary-text-color);
            background: var(--lumo-base-color);
            cursor: pointer;
        }
        .tool:hover {
            color: var(--lumo-body-text-color);
            background: var(--lumo-contrast-10pct);
        }
        .backdrop {
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.45);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 1000;
        }
        .expanded {
            display: flex;
            flex-direction: column;
            background: var(--lumo-base-color);
            border-radius: 10px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.25);
            width: min(1100px, 94vw);
            height: min(80vh, 820px);
            padding: 16px 20px 20px;
        }
        .expanded-head {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 8px;
        }
        .expanded-title {
            flex: 1;
            font-size: 16px;
            font-weight: 600;
            color: var(--lumo-header-text-color);
        }
        .expanded-body {
            flex: 1;
            min-height: 0;
        }
        .dialog {
            background: var(--lumo-base-color);
            border-radius: 10px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.25);
            width: min(480px, calc(100vw - 32px));
            max-height: calc(100vh - 64px);
            overflow-y: auto;
            padding: 20px 24px 24px;
        }
        .dialog .head {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 12px;
        }
        .dialog .title {
            flex: 1;
            font-size: 16px;
            font-weight: 600;
            color: var(--lumo-header-text-color);
        }
        .close {
            width: 20px;
            height: 20px;
            color: var(--lumo-secondary-text-color);
            cursor: pointer;
        }
        .close:hover {
            color: var(--lumo-body-text-color);
        }
        .hidden-list {
            display: flex;
            flex-direction: column;
        }
        .hidden-row {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 6px 0;
            border-top: 1px solid var(--lumo-contrast-5pct);
            font-size: 13px;
            color: var(--lumo-body-text-color);
        }
        .hidden-name {
            flex: 1;
            word-break: break-word;
        }
        .actions {
            display: flex;
            justify-content: flex-end;
            margin-top: 16px;
        }
        .actions button {
            font-size: 13px;
            font-weight: 500;
            padding: 8px 16px;
            border-radius: 6px;
            border: 1px solid var(--lumo-contrast-20pct);
            background: var(--lumo-base-color);
            color: var(--lumo-body-text-color);
            cursor: pointer;
        }
        .actions button:hover {
            filter: brightness(0.97);
        }
        .empty {
            padding: 20px;
            color: var(--lumo-secondary-text-color);
        }
    `;

    static properties = {
        _meters: { state: true },
        _sections: { state: true },
        _expanded: { state: true },
        _hiddenDialogOpen: { state: true }
    };

    constructor() {
        super();
        this._hidden = this._loadHidden();
        this._expanded = null;
        this._hiddenDialogOpen = false;
        // Close an open overlay/dialog with Escape.
        this._keyHandler = (event) => {
            if (event.key !== 'Escape') {
                return;
            }
            if (this._expanded) {
                this._closeExpanded();
            } else if (this._hiddenDialogOpen) {
                this._closeHiddenDialog();
            }
        };
    }

    connectedCallback() {
        super.connectedCallback();
        window.addEventListener('keydown', this._keyHandler);
        // Subscribe to the live meter stream; the backend emits an immediate snapshot then
        // a fresh one every couple of seconds, so no manual refresh is needed.
        this._subscription = this.jsonRpc.streamMeters().onNext(response => {
            const meters = response.result;
            this._meters = meters;
            this._sections = this._buildSections(meters);
        });
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        window.removeEventListener('keydown', this._keyHandler);
        if (this._subscription) {
            this._subscription.cancel();
            this._subscription = null;
        }
    }

    render() {
        if (!this._meters) {
            return html`<span class="empty">Loading meters...</span>`;
        }
        if (this._meters.length === 0) {
            return html`<span class="empty">No meters registered.</span>`;
        }
        const hiddenCount = this._hidden.size;
        return html`
            <div class="toolbar">
                ${hiddenCount > 0 ? html`
                    <button class="hidden-btn" @click=${this._openHiddenDialog} title="Manage hidden metrics">
                        <vaadin-icon icon="font-awesome-solid:eye-slash"></vaadin-icon>
                        ${hiddenCount} hidden
                    </button>` : nothing}
                <span class="live"><span class="dot"></span> Live</span>
            </div>
            ${(this._sections || []).map(section => html`
                <div class="section-heading">
                    ${section.label}<span class="count">${section.total} meters</span>
                </div>
                ${section.bars.length > 0 ? html`
                    <div class="charts">
                        ${section.bars.map(chart => this._barCard(chart))}
                    </div>` : nothing}
                ${section.gauges.length > 0 ? html`
                    <div class="gauges">
                        ${section.gauges.map(gauge => this._gaugeCard(gauge))}
                    </div>` : nothing}`)}
            ${this._expanded ? this._renderExpanded() : nothing}
            ${this._hiddenDialogOpen ? this._renderHiddenDialog() : nothing}`;
    }

    _barCard(chart) {
        return html`
            <div class="card" style="height:${chart.height}px;">
                <div class="card-tools">
                    <vaadin-icon class="tool" title="Expand" icon="font-awesome-solid:expand"
                        @click=${() => this._expand({ kind: 'bar', chart })}></vaadin-icon>
                    <vaadin-icon class="tool" title="Hide" icon="font-awesome-solid:eye-slash"
                        @click=${() => this._hide(chart.key)}></vaadin-icon>
                </div>
                <pui-echart-bar
                    horizontal
                    title="${chart.title}"
                    categories="${chart.categories}"
                    series="${chart.series}"></pui-echart-bar>
            </div>`;
    }

    _gaugeCard(gauge) {
        return html`
            <div class="card gauge">
                <div class="card-tools">
                    <vaadin-icon class="tool" title="Expand" icon="font-awesome-solid:expand"
                        @click=${() => this._expand({ kind: 'gauge', gauge })}></vaadin-icon>
                    <vaadin-icon class="tool" title="Hide" icon="font-awesome-solid:eye-slash"
                        @click=${() => this._hide(gauge.key)}></vaadin-icon>
                </div>
                <pui-echart-gauge
                    title="${gauge.title}"
                    value="${gauge.value}"
                    unit="${gauge.unit}"></pui-echart-gauge>
            </div>`;
    }

    _renderExpanded() {
        const e = this._expanded;
        const inner = e.kind === 'bar'
            ? html`<pui-echart-bar horizontal title="${e.chart.title}"
                categories="${e.chart.categories}" series="${e.chart.series}"></pui-echart-bar>`
            : html`<pui-echart-gauge title="${e.gauge.title}"
                value="${e.gauge.value}" unit="${e.gauge.unit}"></pui-echart-gauge>`;
        const title = e.kind === 'bar' ? e.chart.title : e.gauge.title;
        return html`
            <div class="backdrop" @click=${this._closeExpanded}>
                <div class="expanded" @click=${ev => ev.stopPropagation()}>
                    <div class="expanded-head">
                        <span class="expanded-title">${title}</span>
                        <vaadin-icon class="close" icon="font-awesome-solid:xmark"
                            @click=${this._closeExpanded}></vaadin-icon>
                    </div>
                    <div class="expanded-body">${inner}</div>
                </div>
            </div>`;
    }

    _renderHiddenDialog() {
        const keys = [...this._hidden].sort();
        return html`
            <div class="backdrop" @click=${this._closeHiddenDialog}>
                <div class="dialog" @click=${e => e.stopPropagation()}>
                    <div class="head">
                        <span class="title">Hidden metrics</span>
                        <vaadin-icon class="close" icon="font-awesome-solid:xmark"
                            @click=${this._closeHiddenDialog}></vaadin-icon>
                    </div>
                    ${keys.length === 0 ? html`<div class="empty">Nothing hidden.</div>` : html`
                        <div class="hidden-list">
                            ${keys.map(key => html`
                                <div class="hidden-row">
                                    <span class="hidden-name">${this._labelForKey(key)}</span>
                                    <vaadin-icon class="tool" title="Show" icon="font-awesome-solid:eye"
                                        @click=${() => this._unhide(key)}></vaadin-icon>
                                </div>`)}
                        </div>
                        <div class="actions">
                            <button @click=${this._unhideAll}>Show all</button>
                        </div>`}
                </div>
            </div>`;
    }

    _expand(target) {
        this._expanded = target;
    }

    _closeExpanded() {
        this._expanded = null;
    }

    _openHiddenDialog() {
        this._hiddenDialogOpen = true;
    }

    _closeHiddenDialog() {
        this._hiddenDialogOpen = false;
    }

    _hide(key) {
        this._hidden.add(key);
        this._saveHidden();
        this._rebuild();
    }

    _unhide(key) {
        this._hidden.delete(key);
        this._saveHidden();
        this._rebuild();
    }

    _unhideAll() {
        this._hidden.clear();
        this._saveHidden();
        this._closeHiddenDialog();
        this._rebuild();
    }

    _rebuild() {
        if (this._meters) {
            this._sections = this._buildSections(this._meters);
        }
    }

    _loadHidden() {
        const raw = this.storage.get(PwcMicrometerMetrics.HIDDEN_KEY);
        if (!raw) {
            return new Set();
        }
        try {
            return new Set(JSON.parse(raw));
        } catch (e) {
            return new Set();
        }
    }

    _saveHidden() {
        this.storage.set(PwcMicrometerMetrics.HIDDEN_KEY, JSON.stringify([...this._hidden]));
    }

    // The hide key is "TYPE::name"; the label shown in the manage dialog is just the meter name.
    _labelForKey(key) {
        const sep = key.indexOf('::');
        return sep >= 0 ? key.substring(sep + 2) : key;
    }

    // Split the meters into one section per meter type, in the display order of TYPE_SECTIONS,
    // then a fallback section per any leftover type. Each section charts every one of its families
    // (that is not hidden): multi-series families as bar charts and single-value meters as gauges.
    _buildSections(meters) {
        // Bucket meters by type, then by name within each type.
        const byType = new Map();
        for (const meter of meters) {
            const type = meter.type || 'OTHER';
            if (!byType.has(type)) {
                byType.set(type, new Map());
            }
            const families = byType.get(type);
            if (!families.has(meter.name)) {
                families.set(meter.name, []);
            }
            families.get(meter.name).push(meter);
        }

        const sections = [];
        const seen = new Set();
        const emit = (type, label, stat) => {
            const families = byType.get(type);
            if (!families) {
                return;
            }
            seen.add(type);
            const built = this._buildSection(type, families, stat);
            if (built.bars.length > 0 || built.gauges.length > 0) {
                sections.push({ type, label, total: this._countMeters(families), ...built });
            }
        };

        for (const spec of PwcMicrometerMetrics.TYPE_SECTIONS) {
            emit(spec.type, spec.label, spec.stat);
        }
        // Any meter type we did not anticipate still gets a section, using the generic statistic pick.
        for (const type of byType.keys()) {
            if (!seen.has(type)) {
                emit(type, this._titleCase(type), null);
            }
        }
        return sections;
    }

    // Turn every (non-hidden) family into a chart: a bar chart when it has several tagged series,
    // a gauge when it is a single value. preferredStat picks the statistic to plot.
    _buildSection(type, families, preferredStat) {
        const bars = [];
        const gauges = [];
        for (const [name, group] of families) {
            const key = type + '::' + name;
            if (this._hidden.has(key)) {
                continue;
            }
            if (group.length >= PwcMicrometerMetrics.MIN_SERIES) {
                const chart = this._barChart(key, name, group, preferredStat);
                if (chart) {
                    bars.push(chart);
                }
            } else {
                const gauge = this._gauge(key, name, group[0], preferredStat);
                if (gauge) {
                    gauges.push(gauge);
                }
            }
        }
        // Busiest families first for the bars; gauges alphabetically so they are easy to scan.
        bars.sort((a, b) => JSON.parse(b.categories).length - JSON.parse(a.categories).length);
        gauges.sort((a, b) => a.title.localeCompare(b.title));
        return { bars, gauges };
    }

    _barChart(key, name, group, preferredStat) {
        const bars = group
            .map(meter => ({ label: this._seriesLabel(meter.tags), value: this._statValue(meter, preferredStat) }))
            .filter(bar => bar.value !== null)
            .sort((a, b) => b.value - a.value)
            .slice(0, PwcMicrometerMetrics.MAX_BARS);
        if (bars.length < PwcMicrometerMetrics.MIN_SERIES) {
            return null;
        }
        const unit = group.find(m => m.baseUnit)?.baseUnit;
        // Horizontal bars: order top-to-bottom by putting the largest last (ECharts draws y-axis bottom-up).
        bars.reverse();
        return {
            key: key,
            name: name,
            title: unit ? `${name} (${unit})` : name,
            categories: JSON.stringify(bars.map(b => b.label)),
            series: JSON.stringify([{ name: name, data: bars.map(b => b.value) }]),
            height: Math.min(520, Math.max(220, bars.length * 30 + 80))
        };
    }

    _gauge(key, name, meter, preferredStat) {
        const value = this._statValue(meter, preferredStat);
        if (value === null) {
            return null;
        }
        return { key: key, title: name, value: value, unit: meter.baseUnit || '' };
    }

    // Total number of meters (series) across all families in a type - the "N meters" section count.
    _countMeters(families) {
        let total = 0;
        for (const group of families.values()) {
            total += group.length;
        }
        return total;
    }

    // Pick the numeric value for a meter, preferring the section's statistic (e.g. MAX for timers)
    // and falling back to the most representative generic measurement.
    _statValue(meter, preferredStat) {
        const measurements = meter.measurements || [];
        if (measurements.length === 0) {
            return null;
        }
        const byStatistic = statistic => measurements.find(m => (m.statistic || '').toUpperCase() === statistic);
        if (preferredStat) {
            const preferred = byStatistic(preferredStat);
            if (preferred) {
                return preferred.value;
            }
        }
        const pick = byStatistic('VALUE') || byStatistic('COUNT') || byStatistic('ACTIVE_TASKS')
            || byStatistic('MAX') || byStatistic('TOTAL_TIME') || measurements[0];
        return pick ? pick.value : null;
    }

    // Build a compact label from the tag values (keys are the same across a family, so drop them).
    _seriesLabel(tags) {
        if (!tags) {
            return '(no tags)';
        }
        return tags.split(',')
            .map(pair => {
                const eq = pair.indexOf('=');
                return (eq >= 0 ? pair.substring(eq + 1) : pair).trim();
            })
            .join(' / ');
    }

    // "LONG_TASK_TIMER" -> "Long task timer" for an unexpected type's section heading.
    _titleCase(type) {
        const words = type.toLowerCase().replace(/_/g, ' ');
        return words.charAt(0).toUpperCase() + words.slice(1);
    }
}
customElements.define('pwc-micrometer-metrics', PwcMicrometerMetrics);
