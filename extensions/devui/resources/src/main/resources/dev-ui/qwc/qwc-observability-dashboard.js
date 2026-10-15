import { html, css } from 'lit';
import { html as staticHtml, unsafeStatic } from 'lit/static-html.js';
import { repeat } from 'lit/directives/repeat.js';
import { observabilitySignals } from 'devui-data';
import { devuiState } from 'devui-state';
import { JsonRpc } from 'jsonrpc';
import { StorageController } from 'storage-controller';
import { RouterController } from 'router-controller';
import { ObservabilityCardBase } from 'observability-card-base';
import 'echarts-line';
import 'echarts-histogram';
import 'echarts-gauge';
import '@vaadin/button';
import '@vaadin/icon';
import '@vaadin/tabs';
import '@vaadin/grid';

/**
 * The single "Observability" page: a dashboard of cards the user composes themselves.
 *
 * Two kinds of card are on offer:
 *  - a signal card, for any signal that carries a pageId. The backing page is looked up in
 *    devuiState.unlisted, its component dynamically imported and rendered inline, with a link
 *    out to the full page. That keeps the contract build-time-data only, so a signal
 *    contributed by another extension (traces, from quarkus-opentelemetry) needs no extra
 *    plumbing here.
 *  - a metric chart, one per meter. Metrics is core-owned and has no page of its own; its
 *    signal only advertises that the "devui-observability" JSON-RPC service is available.
 *    How a meter is drawn depends on what was captured for it, not just on its type: see
 *    _plotFor. A counter becomes a rate, a gauge a dial beside its history, a timer its mean
 *    and max, and a meter with a histogram configured gets actual bucket bars.
 *
 * The chosen cards are stored in LocalStorage globally (StorageController defaults to
 * perApp=false) rather than per application, so the dashboard follows the developer between
 * apps. Metric names are inherently app-specific though, so a stored metric card is only
 * rendered while that meter is in the live catalog. The stored list is ordered, and that order
 * is the layout order: cards are dragged by their header to rearrange them. A card can also be
 * maximized to the whole dashboard area, which is where a metric card offers the captured
 * numbers behind its chart as a table (see _renderData).
 *
 * The stored selection is also the source of truth for server-side capture: the metrics
 * backend only samples the meters in its selection, so the dashboard pushes its metric cards
 * through setSelection() on load and on save.
 */
const CONFIG_KEY = 'cards';
const CONFIG_VERSION = 1;
const METRICS_SIGNAL = 'metrics';
const SIGNAL_PREFIX = 'signal:';
const METRIC_PREFIX = 'metric:';

// Meters whose value only ever jumps between whole amounts - the count of something that is
// currently happening. Sloping between two such readings would draw values that never occurred,
// so these are stepped.
const DISCRETE_TYPES = new Set(['LONG_TASK_TIMER', 'LONG_SUM', 'LONG_GAUGE']);

// As many bars as a card-height chart can label legibly; see _fitBuckets.
const MAX_BUCKET_BARS = 14;

// A dial needs to know where "full" is. Micrometer publishes the ceiling of its bounded JVM
// gauges as a meter of its own, so a card showing one of these captures the companion alongside
// it (see _capturedMetricNames) purely to scale the dial. The companion gets no card.
const GAUGE_COMPANION_MAX = {
    'jvm.memory.used': 'jvm.memory.max',
    'jvm.memory.committed': 'jvm.memory.max',
    'jvm.buffer.memory.used': 'jvm.buffer.total.capacity',
    'jvm.gc.live.data.size': 'jvm.gc.max.data.size',
    'process.files.open': 'process.files.max',
};

// Gauges that already report a fraction of one. Micrometer gives these no base unit at all, so
// the name is the only thing that identifies them; the synthetic RATIO_UNIT then gets them
// formatted as a percentage everywhere instead of as "0.42".
const RATIO_METRICS = new Set([
    'system.cpu.usage', 'process.cpu.usage', 'jvm.gc.overhead', 'jvm.memory.usage.after.gc',
]);
const RATIO_UNIT = 'ratio';

// Micrometer and OpenTelemetry spell the same units differently (OTel follows UCUM), and
// neither is what you want on an axis. Map both onto the family the formatter understands.
const UNIT_FAMILIES = {
    s: 'duration', second: 'duration', seconds: 'duration',
    ms: 'duration.ms', millisecond: 'duration.ms', milliseconds: 'duration.ms',
    ns: 'duration.ns', nanosecond: 'duration.ns', nanoseconds: 'duration.ns',
    By: 'bytes', byte: 'bytes', bytes: 'bytes',
};

export class QwcObservabilityDashboard extends ObservabilityCardBase {

    jsonRpc = new JsonRpc(this);
    storageControl = new StorageController(this);

    static styles = css`
        /* border-box: without it the padding is added on top of height:100%, pushing the host
           past its container and giving the page a second scrollbar next to the grid's own. */
        :host { display: flex; flex-direction: column; height: 100%; box-sizing: border-box;
                padding: 15px; gap: 15px; }
        .toolbar { display: flex; gap: 10px; align-items: center; }
        .toolbar .spacer { flex: 1; }
        /* Unsized, a font-awesome icon is drawn at the button's own font size and towers over
           the label of a small button. */
        .toolbar vaadin-button vaadin-icon { width: 14px; height: 14px; }
        .intro { color: var(--lumo-secondary-text-color); }
        .empty { color: var(--lumo-secondary-text-color); padding: 20px; }

        .grid { flex: 1; overflow: auto; display: grid; gap: 15px; align-content: start;
                grid-template-columns: repeat(auto-fill, minmax(440px, 1fr)); }
        /* One card, given the whole dashboard area rather than its usual fixed height. The card
           heights below are overridden, .wide included, by being the more specific selector. */
        .grid.single { grid-template-columns: 1fr; align-content: stretch; overflow: hidden; }
        .grid.single .card { height: auto; }
        .card { display: flex; flex-direction: column; height: 320px;
                border: 1px solid var(--lumo-contrast-10pct); border-radius: var(--lumo-border-radius-m); }
        /* Signal widgets (e.g. the traces tree) are tables, not charts: give them the full row. */
        .card.wide { grid-column: 1 / -1; height: 420px; }
        .card-header { display: flex; align-items: center; gap: 8px; padding: 6px 10px; cursor: grab;
                       border-bottom: 1px solid var(--lumo-contrast-10pct); }
        .card-header:active { cursor: grabbing; }
        .card-header .title { flex: 1; font-weight: bold; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        /* tabular-nums stops the reading jittering sideways as the digits change. */
        .card-header .readout { font-variant-numeric: tabular-nums; white-space: nowrap;
                                color: var(--lumo-primary-text-color); }
        .card-header vaadin-icon { color: var(--lumo-primary-text-color); }
        .card-header vaadin-icon.grip { color: var(--lumo-contrast-30pct); }
        /* Faint until the card is hovered: six cards each shouting an icon is noise. */
        .card-header .zoom { margin: 0; padding: 0; min-width: 0; opacity: 0.35; cursor: pointer; }
        .card-header .zoom vaadin-icon { width: 14px; height: 14px; }
        .card:hover .zoom, .card-header .zoom:focus { opacity: 1; }
        .card.dragging { opacity: 0.4; }
        .card.drop-target { border-color: var(--lumo-primary-color); }
        .card-body { flex: 1; overflow: auto; display: flex; flex-direction: column; min-height: 0; }
        /* The meter name alone does not say what is on the axis - a timer named "latency" is
           charted from its recording count unless it also captured durations - so every metric
           card states its statistic. */
        .caption { padding: 4px 10px 0; font-size: var(--lumo-font-size-xs);
                   color: var(--lumo-secondary-text-color); }
        .caption.percentiles { font-variant-numeric: tabular-nums; color: var(--lumo-body-text-color); }
        echarts-line, echarts-histogram { flex: 1; }
        /* A dial and its history share the card body. The dial is given a fixed share rather than
           an equal one, so the line beside it keeps a time axis wide enough to read. */
        .gauge { flex: 1; display: flex; min-height: 0; }
        .gauge echarts-gauge { flex: 0 0 38%; }
        .gauge echarts-line { flex: 1; min-width: 0; }

        /* Chart/Data only appears on a maximized card, where there is room to read a table. */
        .card-tabs { flex: 0 0 auto; padding: 0 6px; box-shadow: none; }
        /* min-height: the grid is a flex child with its own scroller, so without this it sizes
           itself to its content and pushes the second table off the card instead of scrolling. */
        vaadin-grid.data { flex: 1; min-height: 0; }

        .config { flex: 1; overflow: auto; display: flex; flex-direction: column; gap: 18px; }
        .group-title { font-weight: bold; color: var(--lumo-secondary-text-color);
                       border-bottom: 1px solid var(--lumo-contrast-10pct); padding-bottom: 3px; margin-bottom: 6px; }
        /* A catalog runs to dozens of meters: flow them across the full width rather than
           down one long column, so the whole list stays visible without scrolling. */
        .choices { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 2px 20px; }
        .choice { display: flex; align-items: center; gap: 6px; padding: 2px 0; min-width: 0; }
        .choice-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    `;

    static properties = {
        _cards: { state: true },       // ordered card ids, null until the user has configured
        _configuring: { state: true },
        _draft: { state: true },       // Set of card ids being edited
        _catalog: { state: true },
        _sections: { state: true },    // metric name -> {seriesKey: {tags, source, cumulative, type, points}}
        _dragId: { state: true },      // card id being dragged, null when not reordering
        _dropTarget: { state: true },  // card id the drag is currently hovering
        _maximized: { state: true },   // card id shown on its own, null for the grid
        _cardTab: { state: true },     // 'chart' or 'data', only meaningful while maximized
    };

    constructor() {
        super();
        this.routerController = new RouterController(this);
        this._signals = observabilitySignals ?? [];
        this._metricsAvailable = this._signals.some(s => s.key === METRICS_SIGNAL);
        this._cards = null;
        this._configuring = false;
        this._draft = new Set();
        this._catalog = { groups: [] };
        this._sections = {};
        this._dragId = null;
        this._dropTarget = null;
        // Deliberately not stored alongside the card list: which card you are peering at is a
        // passing state, not part of the dashboard you configured.
        this._maximized = null;
        this._cardTab = 'chart';
        this._retentionMillis = 600000; // overwritten from getCatalog()
        this._tags = new Map();
        // Memoized: echarts-line redraws whenever its valueFormatter changes identity, so handing
        // it a freshly built closure on every render would rebuild every chart on every render.
        this._formatters = new Map();
        this._visHandler = () => this._onVisibilityChange();
        // On document, not on the host: nothing inside a maximized card is focused, so the key
        // never reaches this element.
        this._keyHandler = e => this._onKeyDown(e);
    }

    connectedCallback() {
        super.connectedCallback();
        document.addEventListener('visibilitychange', this._visHandler);
        document.addEventListener('keydown', this._keyHandler);
        this._cards = this._readConfig();
        // First visit: drop straight into the configuration screen, with the embeddable signals
        // already ticked so a developer who just wants "the usual" saves in one click.
        this._configuring = this._cards === null;
        if (this._configuring) {
            this._draft = new Set(this._embeddableSignals().map(s => SIGNAL_PREFIX + s.key));
        } else {
            this._importWidgets();
        }
        if (this._metricsAvailable) {
            this._loadCatalog();
            this._catalogTimer = setInterval(() => this._loadCatalog(), 5000);
            this._pushSelection();
        }
    }

    disconnectedCallback() {
        document.removeEventListener('visibilitychange', this._visHandler);
        document.removeEventListener('keydown', this._keyHandler);
        clearInterval(this._catalogTimer);
        this._unsubscribe();
        super.disconnectedCallback();
    }

    hotReload() {
        this._unsubscribe();
        if (this._metricsAvailable) {
            this._loadCatalog();
            this._pushSelection();
        }
    }

    _onKeyDown(e) {
        if (e.key === 'Escape' && this._maximized !== null) {
            this._maximized = null;
        }
    }

    _toggleMaximized(id) {
        this._maximized = this._maximized === id ? null : id;
        // Always open on the chart: the table is what you go looking for, not what you expect
        // to land on, and a tab left over from the last card would be answering a stale question.
        this._cardTab = 'chart';
    }

    _onVisibilityChange() {
        if (document.hidden) {
            this._unsubscribe(); // stop stream + redraws in background
        } else if (this._metricsAvailable) {
            this._load();        // re-seed from the (preserved) store
            this._subscribe();
        }
    }

    // ---------------------------------------------------------------- configuration

    _readConfig() {
        if (!this.storageControl.has(CONFIG_KEY)) {
            return null;
        }
        try {
            const stored = JSON.parse(this.storageControl.get(CONFIG_KEY));
            return Array.isArray(stored?.cards) ? stored.cards : null;
        } catch (e) {
            // A config written by an older/newer layout is not worth failing over: start fresh.
            this.storageControl.remove(CONFIG_KEY);
            return null;
        }
    }

    _edit() {
        this._draft = new Set(this._cards ?? []);
        this._configuring = true;
    }

    _cancelEdit() {
        this._configuring = false;
    }

    _save() {
        // Keep the previous order for cards that survived the edit, then append the new ones,
        // so editing the dashboard does not reshuffle it.
        const kept = (this._cards ?? []).filter(id => this._draft.has(id));
        const added = [...this._draft].filter(id => !kept.includes(id));
        this._cards = [...kept, ...added];
        this._persist();
        this._configuring = false;
        this._importWidgets();
        if (this._metricsAvailable) {
            // Guarded like every other call site: with no metrics backend there is no
            // "devui-observability" service to answer, and the request would just error.
            this._pushSelection();
        }
    }

    _persist() {
        this.storageControl.set(CONFIG_KEY, JSON.stringify({ version: CONFIG_VERSION, cards: this._cards }));
    }

    _toggleDraft(id, checked) {
        const draft = new Set(this._draft);
        if (checked) {
            draft.add(id);
        } else {
            draft.delete(id);
        }
        this._draft = draft;
    }

    // ---------------------------------------------------------------- cards

    /** Signals that have a page of their own, and so can be embedded as a card. */
    _embeddableSignals() {
        return this._signals.filter(s => s.pageId);
    }

    _signalFor(id) {
        const key = id.substring(SIGNAL_PREFIX.length);
        return this._signals.find(s => s.key === key);
    }

    /** The unlisted Dev UI page backing a signal, carrying the componentRef we import. */
    _pageFor(signal) {
        if (!signal?.pageId) {
            return null;
        }
        return (devuiState.unlisted ?? []).find(p => p.id === signal.pageId) ?? null;
    }

    _importWidgets() {
        (this._cards ?? []).filter(id => id.startsWith(SIGNAL_PREFIX)).forEach(id => {
            const page = this._pageFor(this._signalFor(id));
            if (page?.componentRef) {
                import(page.componentRef);
            }
        });
    }

    /** Memoized so lit-html's static template cache is not invalidated on every render. */
    _tag(name) {
        if (!this._tags.has(name)) {
            this._tags.set(name, unsafeStatic(name));
        }
        return this._tags.get(name);
    }

    _selectedMetricNames() {
        return (this._cards ?? [])
            .filter(id => id.startsWith(METRIC_PREFIX))
            .map(id => id.substring(METRIC_PREFIX.length));
    }

    /**
     * Everything the server should sample: the metric cards, plus the companion "max" meters that
     * tell a dial where full is. A companion is never a card of its own - _visibleCards only ever
     * renders stored ids - so this is deliberately wider than _selectedMetricNames.
     */
    _capturedMetricNames() {
        const names = this._selectedMetricNames();
        const companions = names.map(name => GAUGE_COMPANION_MAX[name]).filter(Boolean);
        return [...new Set([...names, ...companions])];
    }

    /** Metric cards that are actually present in this application's live catalog. */
    _liveMetricNames() {
        const known = new Set((this._catalog.groups ?? []).flatMap(g => g.metrics.map(m => m.name)));
        return this._selectedMetricNames().filter(name => known.has(name));
    }

    /**
     * The stored cards that this application can actually render, in stored order. A card is
     * dropped when the contributing extension is no longer present, or when the meter is not in
     * this application's catalog - the selection is global, so it can name things this app lacks.
     * The entry stays in storage either way, so the card comes back in the app that does have it.
     */
    _visibleCards() {
        const live = new Set(this._liveMetricNames());
        return (this._cards ?? []).filter(id => {
            if (id.startsWith(METRIC_PREFIX)) {
                return live.has(id.substring(METRIC_PREFIX.length));
            }
            return this._pageFor(this._signalFor(id)) !== null;
        });
    }

    // ---------------------------------------------------------------- reordering

    _onDragStart(e, id) {
        this._dragId = id;
        e.dataTransfer.effectAllowed = 'move';
        // Firefox refuses to start a drag unless some data is attached.
        e.dataTransfer.setData('text/plain', id);
        // Drag the whole card, not just the header strip that carries the listener.
        const card = e.currentTarget.closest('.card');
        if (card) {
            e.dataTransfer.setDragImage(card, 20, 20);
        }
    }

    _onDragOver(e, id) {
        if (!this._dragId) {
            return; // something from outside the dashboard: not ours to handle
        }
        e.preventDefault(); // without this the browser never fires drop
        e.dataTransfer.dropEffect = 'move';
        this._dropTarget = id;
    }

    _onDrop(e, id) {
        e.preventDefault();
        this._moveCard(this._dragId, id);
        this._onDragEnd();
    }

    _onDragEnd() {
        this._dragId = null;
        this._dropTarget = null;
    }

    /**
     * Move a card to the position of the one it was dropped on. Indexes are into the full stored
     * list rather than the visible one, so cards hidden in this application keep their place.
     */
    _moveCard(fromId, toId) {
        if (!fromId || fromId === toId) {
            return;
        }
        const cards = [...(this._cards ?? [])];
        const from = cards.indexOf(fromId);
        const to = cards.indexOf(toId);
        if (from < 0 || to < 0) {
            return;
        }
        cards.splice(from, 1);
        cards.splice(to, 0, fromId);
        this._cards = cards;
        this._persist();
    }

    // ---------------------------------------------------------------- metrics data

    _loadCatalog() {
        this.jsonRpc.getCatalog().then(resp => {
            const result = resp.result ?? { groups: [] };
            this._catalog = result;
            if (result.retentionMillis) {
                this._retentionMillis = result.retentionMillis;
            }
        });
    }

    /**
     * Make the server capture exactly the metrics on the dashboard. The backend only samples
     * meters in its selection, and that selection does not survive a new Dev UI session, so the
     * stored dashboard - not client-side state - has to be what drives it.
     */
    _pushSelection() {
        this.jsonRpc.setSelection({ names: this._capturedMetricNames() }).then(() => {
            this._load();
            if (!document.hidden) {
                this._unsubscribe();
                this._subscribe();
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
            if (!s || !this._capturedMetricNames().includes(s.name)) {
                return;
            }
            const section = { ...(this._sections[s.name] ?? {}) };
            const key = this._seriesKey(s);
            const existing = section[key] ?? {
                tags: s.tags, source: s.source, cumulative: s.cumulative, type: s.type,
                unit: s.unit, points: [],
            };
            section[key] = this._appendSample(existing, s);
            this._sections = { ...this._sections, [s.name]: section };
        });
    }

    /**
     * Grow a series by one streamed sample. The distribution columns are parallel to the points,
     * so anything trimmed off the front of the points has to come off them too, or the mean of
     * interval N would be computed from the count of interval N+3.
     */
    _appendSample(existing, s) {
        // Trim to the server's retention window (from getCatalog) so the client never over-retains.
        const cutoff = s.timestamp - this._retentionMillis;
        const points = [...existing.points, [s.timestamp, s.value]];
        const dropped = Math.max(0, points.findIndex(p => p[0] >= cutoff));
        const next = { ...existing, points: points.slice(dropped) };

        if (s.distribution) {
            const held = existing.distribution ?? { totals: [], maxes: [], percentiles: [] };
            next.distribution = {
                totals: [...held.totals, s.distribution.total].slice(dropped),
                maxes: [...held.maxes, s.distribution.max].slice(dropped),
                percentiles: (s.distribution.percentiles ?? []).map((p, i) => ({
                    rank: p.rank,
                    values: [...(held.percentiles[i]?.values ?? []), p.value].slice(dropped),
                })),
                // Buckets are not a per-point column: the store sends the running totals.
                buckets: s.distribution.buckets ?? held.buckets,
            };
        }
        return next;
    }

    _unsubscribe() {
        if (this._stream) {
            this._stream.cancel();
            this._stream = null;
        }
    }

    // ---------------------------------------------------------------- what to draw

    _metricMeta(name) {
        for (const group of this._catalog.groups ?? []) {
            const metric = (group.metrics ?? []).find(m => m.name === name);
            if (metric) {
                return metric;
            }
        }
        return null;
    }

    /**
     * Decide how a meter should be drawn and what the card should say it is showing. This is
     * driven by what the store captured, not by the meter type alone: the same timer renders as
     * bucket bars when a histogram is configured on it and as a mean/max line when it is not.
     *
     * The distinction matters for more than looks. The primary value of a timer, summary or
     * histogram is its RECORDING COUNT, so drawing every meter as one line silently turns a
     * latency meter into a throughput chart. Every kind below states its statistic in the
     * caption for that reason.
     */
    _plotFor(name) {
        const series = Object.values(this._sections[name] ?? {});
        const meta = this._metricMeta(name) ?? {};
        const type = (series[0]?.type ?? meta.type ?? '').toUpperCase();
        const unit = RATIO_METRICS.has(name) ? RATIO_UNIT : (series[0]?.unit ?? meta.unit ?? null);
        const cumulative = series[0]?.cumulative ?? meta.cumulative ?? false;

        if (series.some(s => s.distribution?.buckets?.boundaries?.length)) {
            return { kind: 'histogram', unit, caption: 'Recordings per bucket, since capture started' };
        }
        if (series.some(s => s.distribution)) {
            const percentiles = series.some(s => s.distribution?.percentiles?.length);
            return {
                kind: 'distribution',
                unit,
                caption: percentiles ? 'Mean, max and percentiles per interval' : 'Mean and max per interval',
            };
        }
        if (type === 'LONG_TASK_TIMER') {
            return { kind: 'step', unit, caption: 'Tasks running' };
        }
        if (cumulative) {
            // The value is a running total, so the interesting quantity is how fast it grows.
            return { kind: 'rate', unit, caption: `${this._rateNoun(unit)} per second` };
        }
        // A reading that stands on its own: draw it on a dial, with its history beside it. Only
        // when the dial has a scale to point at - see _gaugeBound - otherwise it is a line as before.
        const step = DISCRETE_TYPES.has(type);
        const bound = this._gaugeBound(name, series, unit);
        if (bound) {
            return { kind: 'gauge', unit, step, bound, caption: `Current value, ${bound.caption}` };
        }
        // No unit in any caption: the axis labels and the header reading both carry it already.
        return { kind: step ? 'step' : 'value', unit, caption: 'Current value' };
    }

    /**
     * The scale for a gauge dial, or null to leave the card as a line chart. A dial shows one
     * reading, so a gauge spread across tag combinations keeps its lines - several needles on one
     * card would say less than several lines do.
     *
     * Where the maximum comes from, in descending order of how much it can be trusted: the meter
     * is already a ratio; a companion meter publishes the real ceiling; or, failing both, the
     * largest reading in the retention window. The last is not an absolute scale and the caption
     * says so, but "where is it against its recent peak" is still worth a dial.
     */
    _gaugeBound(name, series, unit) {
        if (series.length !== 1) {
            return null;
        }
        if (unit === RATIO_UNIT) {
            return { min: 0, max: 1, caption: 'out of 100%' };
        }
        const companion = this._companionMax(name, series[0]);
        if (companion) {
            return { min: 0, max: companion.value, caption: `out of ${companion.name}` };
        }
        const values = series[0].points.map(p => p[1]).filter(v => Number.isFinite(v));
        if (values.length === 0) {
            return null;
        }
        return {
            min: Math.min(0, ...values),
            max: this._niceCeiling(Math.max(...values)),
            caption: 'dial scaled to the peak so far',
        };
    }

    /**
     * The current reading of the meter that says where this gauge's full scale is, matched on tags
     * so a per-pool gauge is scaled by its own pool's ceiling. Null when the companion has not been
     * captured yet, or when the JVM reports no limit (which it does as -1).
     */
    _companionMax(name, series) {
        const companionName = GAUGE_COMPANION_MAX[name];
        if (!companionName) {
            return null;
        }
        const match = Object.values(this._sections[companionName] ?? {})
            .find(candidate => this._sameTags(candidate.tags, series.tags));
        const value = match ? this._last(match.points) : null;
        return value === null || value <= 0 ? null : { name: companionName, value };
    }

    _sameTags(a, b) {
        const keys = Object.keys(a ?? {});
        return keys.length === Object.keys(b ?? {}).length && keys.every(k => a[k] === b[k]);
    }

    /** Round up to 1, 2 or 5 times a power of ten, so a dial ends on a number worth reading. */
    _niceCeiling(value) {
        if (!(value > 0)) {
            return 1; // nothing has been recorded above zero yet; any positive scale will do
        }
        const magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        const scaled = value / magnitude;
        const step = scaled <= 1 ? 1 : scaled <= 2 ? 2 : scaled <= 5 ? 5 : 10;
        return step * magnitude;
    }

    _rateNoun(unit) {
        if (!unit || unit === '1') {
            return 'Events';
        }
        return unit.charAt(0).toUpperCase() + unit.slice(1);
    }

    /** The single number worth putting in the header, or null when several series share the card. */
    _readout(name, plot) {
        const series = Object.values(this._sections[name] ?? {});
        if (plot.kind === 'histogram') {
            const total = series.reduce(
                (sum, s) => sum + (s.distribution?.buckets?.counts ?? []).reduce((a, b) => a + b, 0), 0);
            return total > 0 ? `${this._compact(total)} recorded` : null;
        }
        // With more than one tag combination there is no single current value to show; the chart
        // legend is what tells them apart.
        if (series.length !== 1) {
            return null;
        }
        const s = series[0];
        const format = this._formatter(plot);
        if (plot.kind === 'rate') {
            return `${this._compact(this._last(this._toRate(s.points)))}/s`;
        }
        if (plot.kind === 'distribution') {
            const mean = this._last(this._toIntervalMean(s.points, s.distribution?.totals ?? []));
            return mean === null ? null : format(mean);
        }
        const value = this._last(s.points);
        return value === null ? null : format(value);
    }

    /** Last non-null y value of a [[ts, v], ...] list. */
    _last(points) {
        for (let i = points.length - 1; i >= 0; i--) {
            if (points[i][1] !== null && Number.isFinite(points[i][1])) {
                return points[i][1];
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- formatting

    /**
     * A value formatter for a plot's unit, shared by its axis, its tooltip and its header
     * readout so the three never disagree. Memoized on the unit: see _formatters.
     */
    _formatter(plot) {
        const unit = plot.unit ?? '';
        if (!this._formatters.has(unit)) {
            this._formatters.set(unit, v => this._formatValue(v, unit));
        }
        return this._formatters.get(unit);
    }

    /**
     * A formatter for the number at the end of a dial's arc, where there is room for a magnitude
     * but not for a word. Units like KiB or ms are the magnitude and have to stay; a unit that is
     * only a noun ("files", "threads") is already said by the reading in the middle of the dial.
     * Memoized alongside the value formatters, under a key that cannot collide with a unit.
     */
    _boundFormatter(plot) {
        const unit = plot.unit ?? '';
        if (unit === RATIO_UNIT || UNIT_FAMILIES[unit]) {
            return this._formatter(plot);
        }
        const key = `bound:${unit}`;
        if (!this._formatters.has(key)) {
            this._formatters.set(key, v => this._compact(v));
        }
        return this._formatters.get(key);
    }

    _formatValue(value, unit) {
        if (value === null || value === undefined || !Number.isFinite(value)) {
            return '-';
        }
        if (unit === RATIO_UNIT) {
            return `${this._compact(value * 100)}%`;
        }
        const family = UNIT_FAMILIES[unit];
        if (family === 'duration' || family === 'duration.ms' || family === 'duration.ns') {
            const seconds = family === 'duration' ? value
                : family === 'duration.ms' ? value / 1e3 : value / 1e9;
            return this._formatDuration(seconds);
        }
        if (family === 'bytes') {
            return this._formatBytes(value);
        }
        const compact = this._compact(value);
        return unit && unit !== '1' ? `${compact} ${unit}` : compact;
    }

    /** Durations are captured in seconds, but in dev they are usually milliseconds or less. */
    _formatDuration(seconds) {
        const abs = Math.abs(seconds);
        if (abs === 0) {
            return '0';
        }
        if (abs < 1e-3) {
            return `${this._compact(seconds * 1e6)} µs`;
        }
        if (abs < 1) {
            return `${this._compact(seconds * 1e3)} ms`;
        }
        if (abs < 60) {
            return `${this._compact(seconds)} s`;
        }
        return `${this._compact(seconds / 60)} min`;
    }

    _formatBytes(bytes) {
        const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB'];
        let value = bytes;
        let i = 0;
        while (Math.abs(value) >= 1024 && i < units.length - 1) {
            value /= 1024;
            i++;
        }
        return `${this._compact(value)} ${units[i]}`;
    }

    /** Three significant digits, without the trailing zeros toPrecision leaves behind. */
    _compact(value) {
        if (!Number.isFinite(value)) {
            return '-';
        }
        if (Number.isInteger(value) && Math.abs(value) < 1e6) {
            return String(value);
        }
        return String(parseFloat(value.toPrecision(3)));
    }

    // ---------------------------------------------------------------- series building

    /** Build echarts-line `series` JSON for a metric: one or more lines per tag combination. */
    _seriesFor(name, plot) {
        const byKey = this._sections[name] ?? {};
        const lines = [];
        Object.values(byKey).forEach(s => {
            const label = this._legend(s);
            if (plot.kind === 'distribution') {
                lines.push(...this._distributionLines(s, label));
            } else {
                lines.push({ name: label, data: plot.kind === 'rate' ? this._toRate(s.points) : s.points });
            }
        });
        return JSON.stringify(lines);
    }

    /**
     * Mean and max (and any published percentiles) for one distribution series. The max is drawn
     * dashed so it reads as the outlier bound of the same measurement rather than a second,
     * unrelated line. A max is only plotted when the backend actually tracks one - a Micrometer
     * FunctionTimer, for instance, knows only totals.
     */
    _distributionLines(s, label) {
        const totals = s.distribution?.totals ?? [];
        const lines = [{ name: `${label} mean`, data: this._toIntervalMean(s.points, totals) }];

        const maxes = s.distribution?.maxes ?? [];
        if (maxes.some(m => Number.isFinite(m))) {
            lines.push({
                name: `${label} max`,
                dashed: true,
                data: s.points.map((p, i) => [p[0], Number.isFinite(maxes[i]) ? maxes[i] : null]),
            });
        }
        (s.distribution?.percentiles ?? []).forEach(p => {
            lines.push({
                name: `${label} ${this._rankLabel(p.rank)}`,
                data: s.points.map((point, i) => [point[0], p.values[i] ?? null]),
            });
        });
        return lines.map(line => ({ ...line, showSymbol: this._isSparse(line.data) }));
    }

    /**
     * Whether a line has few enough real points that it should be drawn with markers. Intervals
     * with no recordings leave gaps, and under the intermittent traffic of a dev session most of
     * a mean line can be gaps - drawn as a bare line, isolated points are invisible.
     */
    _isSparse(data) {
        let drawn = 0;
        for (let i = 1; i < data.length; i++) {
            // A segment is only drawn where two consecutive points both have a value.
            if (data[i][1] !== null && data[i - 1][1] !== null) {
                drawn++;
            }
        }
        return drawn < data.length / 2;
    }

    /** The latest published percentiles, for a card whose chart has no room to draw them. */
    _percentileSummary(name, plot) {
        const series = Object.values(this._sections[name] ?? {});
        const percentiles = series[0]?.distribution?.percentiles ?? [];
        if (series.length !== 1 || percentiles.length === 0) {
            return null;
        }
        const format = this._formatter(plot);
        const parts = percentiles
            .map(p => {
                const value = [...p.values].reverse().find(v => Number.isFinite(v) && v > 0);
                return value === undefined ? null : `${this._rankLabel(p.rank)} ${format(value)}`;
            })
            .filter(part => part !== null);
        return parts.length === 0 ? null : parts.join(' · ');
    }

    /**
     * The mean of each sampling interval: how much was recorded in it divided by how many
     * recordings there were. Both figures are cumulative, so this differences them exactly the
     * way _toRate differences a counter. An interval with no recordings has no mean, and is left
     * as a gap rather than drawn as zero - zero would read as "it got fast".
     */
    _toIntervalMean(points, totals) {
        const out = [];
        for (let i = 1; i < points.length; i++) {
            const recordings = points[i][1] - points[i - 1][1];
            const amount = totals[i] - totals[i - 1];
            out.push([points[i][0], recordings > 0 ? amount / recordings : null]);
        }
        return out;
    }

    /**
     * Bucket bars for a histogram card, summed over the tag combinations so the card shows one
     * distribution rather than several overlapping ones. Series whose buckets were configured
     * differently cannot be summed, so only those matching the first series are included.
     */
    _bucketsFor(name, plot) {
        const series = Object.values(this._sections[name] ?? {})
            .filter(s => s.distribution?.buckets?.boundaries?.length);
        if (series.length === 0) {
            return '[]';
        }
        const boundaries = series[0].distribution.buckets.boundaries;
        const signature = JSON.stringify(boundaries);
        const totals = new Array(boundaries.length + 1).fill(0);
        series
            .filter(s => JSON.stringify(s.distribution.buckets.boundaries) === signature)
            .forEach(s => s.distribution.buckets.counts.forEach((c, i) => {
                totals[i] += c;
            }));

        // lower/upper null means the bucket is open at that end: the first and the overflow.
        const buckets = totals.map((count, i) => ({
            lower: i === 0 ? null : boundaries[i - 1],
            upper: i < boundaries.length ? boundaries[i] : null,
            count,
        }));

        const format = this._formatter(plot);
        return JSON.stringify(this._fitBuckets(buckets).map(b => ({
            label: b.lower === null ? `≤ ${format(b.upper)}`
                : b.upper === null ? `> ${format(b.lower)}`
                    : `${format(b.lower)} - ${format(b.upper)}`,
            count: b.count,
        })));
    }

    /**
     * Reduce a bucket list to something a card-sized chart can actually show. Micrometer's
     * publishPercentileHistogram emits around seventy buckets, most of them empty for any given
     * workload, which would be seventy unreadable rows. Drop the empty ends first, since they
     * carry no information, and only merge neighbours if what is left is still too long.
     */
    _fitBuckets(buckets) {
        const first = buckets.findIndex(b => b.count > 0);
        if (first < 0) {
            return []; // nothing recorded yet
        }
        let last = buckets.length - 1;
        while (buckets[last].count === 0) {
            last--;
        }
        const populated = buckets.slice(first, last + 1);
        if (populated.length <= MAX_BUCKET_BARS) {
            return populated;
        }
        const groupSize = Math.ceil(populated.length / MAX_BUCKET_BARS);
        const merged = [];
        for (let i = 0; i < populated.length; i += groupSize) {
            const group = populated.slice(i, i + groupSize);
            merged.push({
                lower: group[0].lower,
                upper: group[group.length - 1].upper,
                count: group.reduce((sum, b) => sum + b.count, 0),
            });
        }
        return merged;
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
            // Only the captured points go: the server keeps the catalog and the selection, so the
            // cards stay put and simply start filling again from the next sample.
            this._sections = {};
        });
    }

    /**
     * One row per captured point, for the metrics that have a card. "value" is the raw primary
     * statistic, which for a timer or summary is the recording count - "total" and "max" are what
     * was actually measured. Every row carries the same columns, blank where the meter has no
     * distribution, because the CSV header is taken from the first row.
     */
    _export() {
        const rows = [];
        const carded = new Set(this._selectedMetricNames());
        Object.entries(this._sections).filter(([name]) => carded.has(name)).forEach(([name, byKey]) => {
            Object.values(byKey).forEach(s => {
                const totals = s.distribution?.totals ?? [];
                const maxes = s.distribution?.maxes ?? [];
                s.points.forEach(([ts, val], i) => {
                    rows.push({
                        name,
                        series: this._legend(s),
                        timestamp: ts,
                        unit: s.unit ?? '',
                        value: val,
                        total: totals[i] ?? '',
                        max: Number.isFinite(maxes[i]) ? maxes[i] : '',
                    });
                });
            });
        });
        this.exportCsv(rows, 'metrics.csv');
    }

    // ---------------------------------------------------------------- render

    render() {
        return this._configuring ? this._renderConfig() : this._renderDashboard();
    }

    _renderConfig() {
        const signals = this._embeddableSignals();
        return html`
            <div class="toolbar">
                <span class="intro">Pick what you want on your observability dashboard. Your choice is
                    remembered in this browser and used for every application.</span>
            </div>
            <div class="config">
                ${signals.length === 0 ? '' : html`
                    <div>
                        <div class="group-title">Signals</div>
                        <div class="choices">
                            ${signals.map(s => this._renderChoice(SIGNAL_PREFIX + s.key, s.title, s.title))}
                        </div>
                    </div>`}
                ${(this._catalog.groups ?? []).map(g => html`
                    <div>
                        <div class="group-title">${g.group}</div>
                        <div class="choices">
                            ${g.metrics.map(m => this._renderChoice(METRIC_PREFIX + m.name,
                                html`${m.name} <small>(${m.type.toLowerCase()})</small>`, m.name))}
                        </div>
                    </div>`)}
            </div>
            <div class="toolbar">
                <span class="intro">${this._draft.size} selected</span>
                <span class="spacer"></span>
                ${this._cards === null ? '' : html`
                    <vaadin-button theme="small" @click=${this._cancelEdit}>
                        <vaadin-icon icon="font-awesome-solid:xmark" slot="prefix"></vaadin-icon>
                        Cancel
                    </vaadin-button>`}
                <vaadin-button theme="primary small" @click=${this._save}>
                    <vaadin-icon icon="font-awesome-solid:floppy-disk" slot="prefix"></vaadin-icon>
                    Save
                </vaadin-button>
            </div>`;
    }

    // tooltip carries the full text, as a long meter name is ellipsized to keep the grid aligned.
    _renderChoice(id, label, tooltip) {
        return html`
            <label class="choice" title="${tooltip}">
                <input type="checkbox" .checked=${this._draft.has(id)}
                       @change=${e => this._toggleDraft(id, e.target.checked)}>
                <span class="choice-label">${label}</span>
            </label>`;
    }

    _renderDashboard() {
        const cards = this._visibleCards();
        // A maximized card that has since left the catalog falls back to the grid rather than
        // leaving the page showing nothing.
        const maximized = cards.includes(this._maximized) ? this._maximized : null;
        const shown = maximized === null ? cards : [maximized];
        return html`
            <div class="toolbar">
                ${maximized === null ? '' : html`
                    <span class="intro">Press Escape to return to the dashboard.</span>`}
                <span class="spacer"></span>
                ${this._metricsAvailable ? html`
                    <vaadin-button theme="small" @click=${this._clear}
                                   title="Discard the points captured so far and start the charts again">
                        <vaadin-icon icon="font-awesome-solid:eraser" slot="prefix"></vaadin-icon>
                        Clear history
                    </vaadin-button>
                    <vaadin-button theme="small" @click=${this._export}>
                        <vaadin-icon icon="font-awesome-solid:file-csv" slot="prefix"></vaadin-icon>
                        Export CSV
                    </vaadin-button>` : ''}
                <vaadin-button theme="small" @click=${this._edit}>
                    <vaadin-icon icon="font-awesome-solid:sliders" slot="prefix"></vaadin-icon>
                    Edit dashboard
                </vaadin-button>
            </div>
            ${cards.length === 0
                ? html`<div class="empty">Your dashboard is empty. Use "Edit dashboard" to add cards.</div>`
                // Keyed, so a reorder moves the existing DOM: an embedded widget keeps its state
                // instead of being torn down and rebuilt on every drop.
                : html`
                    <div class="grid ${maximized === null ? '' : 'single'}">
                        ${repeat(shown, id => id, id => this._renderCard(id))}
                    </div>`}`;
    }

    _renderCard(id) {
        return id.startsWith(SIGNAL_PREFIX) ? this._renderSignalCard(id) : this._renderMetricCard(id);
    }

    /** The header doubles as the drag handle, so the card body stays interactive. */
    _renderCardHeader(id, content) {
        // Nothing to reorder against while one card has the page to itself, and the grip would
        // invite a drag that could not go anywhere.
        const maximized = this._maximized === id;
        return html`
            <div class="card-header" draggable="${maximized ? 'false' : 'true'}"
                 @dragstart=${e => this._onDragStart(e, id)}
                 @dragend=${this._onDragEnd}>
                ${maximized ? '' : html`
                    <vaadin-icon class="grip" icon="font-awesome-solid:grip-vertical"></vaadin-icon>`}
                ${content}
                <vaadin-button theme="tertiary small icon" class="zoom"
                               title="${maximized ? 'Back to the dashboard' : 'Show this card on its own'}"
                               @click=${() => this._toggleMaximized(id)}>
                    <vaadin-icon icon="font-awesome-solid:${maximized ? 'compress' : 'expand'}"></vaadin-icon>
                </vaadin-button>
            </div>`;
    }

    _cardClass(id, wide) {
        return 'card'
            + (wide ? ' wide' : '')
            + (this._dragId === id ? ' dragging' : '')
            + (this._dropTarget === id && this._dragId !== id ? ' drop-target' : '');
    }

    _renderSignalCard(id) {
        const signal = this._signalFor(id);
        const page = this._pageFor(signal);
        const tag = this._tag(page.componentName);
        return html`
            <div class="${this._cardClass(id, true)}"
                 @dragover=${e => this._onDragOver(e, id)} @drop=${e => this._onDrop(e, id)}>
                ${this._renderCardHeader(id, html`
                    <vaadin-icon icon="${signal.icon}"></vaadin-icon>
                    <span class="title">${signal.title}</span>
                    <vaadin-button theme="tertiary small"
                                   @click=${() => this.routerController.goToPath('/' + page.id)}>Open</vaadin-button>`)}
                <div class="card-body">${staticHtml`<${tag}></${tag}>`}</div>
            </div>`;
    }

    _renderMetricCard(id) {
        const name = id.substring(METRIC_PREFIX.length);
        const plot = this._plotFor(name);
        const readout = this._readout(name, plot);
        // The numbers behind the chart are only offered on a maximized card: a table needs the
        // room, and in the grid the chart is the whole point of the tile.
        const maximized = this._maximized === id;
        return html`
            <div class="${this._cardClass(id, false)}"
                 @dragover=${e => this._onDragOver(e, id)} @drop=${e => this._onDrop(e, id)}>
                ${this._renderCardHeader(id, html`
                    <span class="title" title="${name}">${name}</span>
                    ${readout === null ? '' : html`<span class="readout">${readout}</span>`}`)}
                <div class="card-body">
                    ${maximized ? this._renderCardTabs() : ''}
                    ${maximized && this._cardTab === 'data'
                        ? this._renderData(name, plot)
                        : html`
                            <div class="caption">${plot.caption}</div>
                            ${this._renderPercentileSummary(name, plot)}
                            ${this._renderPlot(name, plot)}`}
                </div>
            </div>`;
    }

    _renderCardTabs() {
        return html`
            <vaadin-tabs class="card-tabs" theme="small" selected=${this._cardTab === 'data' ? 1 : 0}>
                <vaadin-tab @click=${() => { this._cardTab = 'chart'; }}>Chart</vaadin-tab>
                <vaadin-tab @click=${() => { this._cardTab = 'data'; }}>Data</vaadin-tab>
            </vaadin-tabs>`;
    }

    /**
     * The numbers as captured, rather than as charted: no rates, means or merged buckets, so this
     * is also where you go to see what a derived line was computed from. Values are unrounded and
     * carry the meter's own unit, which is why the headers name it - a duration here is in seconds
     * even though the chart labels it in milliseconds.
     */
    _renderData(name, plot) {
        const series = Object.values(this._sections[name] ?? {});
        if (series.length === 0) {
            return html`<div class="empty">Nothing captured yet.</div>`;
        }
        const buckets = this._rawBuckets(series);
        const unit = series[0].unit && series[0].unit !== '1' ? ` (${series[0].unit})` : '';
        const ranks = series.find(s => s.distribution?.percentiles?.length)?.distribution.percentiles ?? [];
        const distribution = series.some(s => s.distribution);
        return html`
            ${buckets === null ? '' : html`
                <div class="caption">
                    Buckets as published, counted since capture started. The chart drops the empty
                    ends and merges neighbours to fit; nothing is merged here.
                </div>
                <vaadin-grid class="data" theme="compact no-border row-stripes" .items=${buckets}>
                    <vaadin-grid-column path="range" header="Bucket${unit}"></vaadin-grid-column>
                    <vaadin-grid-column path="count" header="Count" width="120px" flex-grow="0"></vaadin-grid-column>
                </vaadin-grid>`}
            <div class="caption">Captured samples, newest first</div>
            <vaadin-grid class="data" theme="compact no-border row-stripes"
                         .items=${this._sampleRows(series)}>
                <vaadin-grid-column path="when" header="Time" width="110px" flex-grow="0"></vaadin-grid-column>
                ${series.length === 1 ? '' : html`
                    <vaadin-grid-column path="series" header="Series"></vaadin-grid-column>`}
                <vaadin-grid-column path="value" header="${this._valueHeader(plot, unit)}"></vaadin-grid-column>
                ${!distribution ? '' : html`
                    <vaadin-grid-column path="total" header="Total${unit}"></vaadin-grid-column>
                    <vaadin-grid-column path="max" header="Max${unit}"></vaadin-grid-column>`}
                ${ranks.map(p => html`
                    <vaadin-grid-column path="${this._rankKey(p.rank)}"
                                        header="${this._rankLabel(p.rank)}${unit}"></vaadin-grid-column>`)}
            </vaadin-grid>`;
    }

    /**
     * What the meter's primary value actually is. For a timer or a summary it is the recording
     * count, not the measurement, which is the same trap the chart captions guard against - and
     * a count is unitless, however the meter's own measurements are denominated.
     */
    _valueHeader(plot, unit) {
        return plot.kind === 'histogram' || plot.kind === 'distribution' ? 'Recordings' : `Value${unit}`;
    }

    /**
     * The row property a percentile column is read from. A vaadin-grid path is split on dots to
     * walk nested properties, so "p99.9" would be resolved as row.p99["9"] and the column would
     * come out blank; the label keeps the dot, the key cannot have one.
     */
    _rankKey(rank) {
        return this._rankLabel(rank).replace('.', '_');
    }

    /** 0.95 -> "p95", without the floating point tail that rank * 100 can leave behind. */
    _rankLabel(rank) {
        return `p${parseFloat((rank * 100).toPrecision(6))}`;
    }

    /** One row per captured point per series, newest first so the live end needs no scrolling. */
    _sampleRows(series) {
        const rows = [];
        series.forEach(s => {
            const totals = s.distribution?.totals ?? [];
            const maxes = s.distribution?.maxes ?? [];
            const percentiles = s.distribution?.percentiles ?? [];
            s.points.forEach(([ts, value], i) => {
                const row = {
                    ts,
                    when: new Date(ts).toLocaleTimeString(),
                    series: this._legend(s),
                    value,
                    total: totals[i] ?? '',
                    max: Number.isFinite(maxes[i]) ? maxes[i] : '',
                };
                percentiles.forEach(p => {
                    row[this._rankKey(p.rank)] = p.values[i] ?? '';
                });
                rows.push(row);
            });
        });
        return rows.sort((a, b) => b.ts - a.ts);
    }

    /**
     * Every published bucket, summed over the tag combinations the way the chart sums them, or
     * null when the meter publishes none. Unlike _bucketsFor this keeps the empty buckets: an
     * empty range is a fact about the distribution, and in a table it costs only a row.
     */
    _rawBuckets(series) {
        const withBuckets = series.filter(s => s.distribution?.buckets?.boundaries?.length);
        if (withBuckets.length === 0) {
            return null;
        }
        const boundaries = withBuckets[0].distribution.buckets.boundaries;
        const signature = JSON.stringify(boundaries);
        const totals = new Array(boundaries.length + 1).fill(0);
        withBuckets
            .filter(s => JSON.stringify(s.distribution.buckets.boundaries) === signature)
            .forEach(s => s.distribution.buckets.counts.forEach((c, i) => {
                totals[i] += c;
            }));
        return totals.map((count, i) => ({
            range: i === 0 ? `≤ ${boundaries[0]}`
                : i < boundaries.length ? `${boundaries[i - 1]} - ${boundaries[i]}`
                    : `> ${boundaries[boundaries.length - 1]}`,
            count,
        }));
    }

    /**
     * A meter can publish percentiles and a histogram at once. The histogram takes the chart, so
     * the percentiles are shown as a reading instead of being dropped.
     */
    _renderPercentileSummary(name, plot) {
        if (plot.kind !== 'histogram') {
            return ''; // the distribution chart draws them as lines
        }
        const summary = this._percentileSummary(name, plot);
        return summary === null ? '' : html`<div class="caption percentiles">${summary}</div>`;
    }

    _renderPlot(name, plot) {
        if (plot.kind === 'histogram') {
            return html`<echarts-histogram buckets=${this._bucketsFor(name, plot)}></echarts-histogram>`;
        }
        const format = this._formatter(plot);
        if (plot.kind === 'gauge') {
            // The dial answers "how is it right now", the line beside it "how did it get there".
            // The legend is dropped: a dial card is single-series by definition, so the only thing
            // it could name is the card's own title.
            const points = Object.values(this._sections[name] ?? {})[0]?.points ?? [];
            return html`
                <div class="gauge">
                    <echarts-gauge .value=${this._last(points) ?? 0}
                                   .min=${plot.bound.min} .max=${plot.bound.max}
                                   .valueFormatter=${format}
                                   .boundFormatter=${this._boundFormatter(plot)}></echarts-gauge>
                    <echarts-line series=${this._seriesFor(name, plot)}
                                  ?step=${plot.step === true}
                                  .showLegend=${false}
                                  .valueFormatter=${format}></echarts-line>
                </div>`;
        }
        return html`
            <echarts-line series=${this._seriesFor(name, plot)}
                          ?step=${plot.kind === 'step'}
                          .valueFormatter=${format}></echarts-line>`;
    }
}
customElements.define('qwc-observability-dashboard', QwcObservabilityDashboard);
