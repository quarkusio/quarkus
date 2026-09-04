package io.quarkus.devui.observability.store.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

/**
 * Per-series, time-windowed store for the Dev UI metrics view. On {@link #observe} it
 * always refreshes the catalog (cheap), and — only for names in the current selection —
 * appends a point to that series' ring, evicts points older than the retention window,
 * and best-effort broadcasts the sample to live subscribers (drop-on-overflow, so a slow
 * Dev UI socket never blocks the capture thread). Selective capture keeps memory and CPU
 * proportional to what the user is actually looking at.
 * <p>
 * Pure — mutiny only, no Vert.x/CDI. It also carries the sampling interval (a plain long)
 * so capture adapters can read it off the injected store without a config dependency, and
 * so the service can advertise it to the client. JSON shaping of {@link #snapshot()} /
 * {@link #catalog()} is done by the JSON-RPC service in the {@code quarkus-devui} runtime.
 */
public final class MetricsTimeSeriesStore {

    private final long retentionMillis;
    private final int maxPointsPerSeries;
    private final long sampleIntervalMillis;

    private final MetricCatalog catalog = new MetricCatalog();
    private final java.util.Set<String> selection = ConcurrentHashMap.newKeySet();
    private final Map<String, MetricSeries> series = new ConcurrentHashMap<>();
    private final BroadcastProcessor<MetricSample> broadcaster = BroadcastProcessor.create();

    public MetricsTimeSeriesStore(long retentionMillis, int maxPointsPerSeries, long sampleIntervalMillis) {
        this.retentionMillis = retentionMillis;
        this.maxPointsPerSeries = maxPointsPerSeries;
        this.sampleIntervalMillis = sampleIntervalMillis;
    }

    public void observe(MetricSample s) {
        if (!Double.isFinite(s.value())) {
            return; // skip NaN/Infinity gauges
        }
        catalog.record(s); // always, for the picker
        if (!selection.contains(s.name())) {
            return; // not selected -> no history, no stream
        }
        MetricSeries ser = series.computeIfAbsent(s.seriesKey(),
                k -> new MetricSeries(s.name(), s.tags(), s.type(), s.cumulative(), s.source(), maxPointsPerSeries));
        ser.add(s.timestampMillis(), s.value());
        ser.evictOlderThan(s.timestampMillis() - retentionMillis);
        broadcaster.onNext(s);
    }

    public MetricCatalog catalog() {
        return catalog;
    }

    public void setSelection(Collection<String> names) {
        selection.clear();
        selection.addAll(names);
        // Drop stored series whose name is no longer selected, freeing memory immediately.
        series.entrySet().removeIf(e -> !selection.contains(e.getValue().name));
    }

    /** Ordered (by series key) plain-Java snapshot of the selected series; service groups by name. */
    public List<MetricSeriesSnapshot> snapshot() {
        List<MetricSeriesSnapshot> out = new ArrayList<>();
        Map<String, MetricSeries> ordered = new TreeMap<>(series);
        for (MetricSeries ser : ordered.values()) {
            out.add(ser.snapshot());
        }
        return out;
    }

    public Multi<MetricSample> stream() {
        return broadcaster.onOverflow().drop();
    }

    /** Number of currently stored (selected) series. */
    public int seriesCount() {
        return series.size();
    }

    /** Number of meters available to chart (catalog size); backs the signal tile. */
    public int meterCount() {
        return catalog.size();
    }

    /** Retention window / sampling interval, advertised to capture adapters and the client. */
    public long retentionMillis() {
        return retentionMillis;
    }

    public long sampleIntervalMillis() {
        return sampleIntervalMillis;
    }

    /**
     * Full manual reset (the Dev UI "Clear" action): drops captured history, the catalog, and the
     * current selection, returning the store to its initial empty state. The catalog and any
     * re-selected series repopulate from the next sample cycle.
     */
    public void clear() {
        series.clear();
        selection.clear();
        catalog.clear();
    }

    /**
     * Drops only the catalog metadata, keeping the selection and captured history. Called once per
     * live reload (from {@code MetricsStoreProducer}) so the picker reflects only the meters present
     * after the reload rather than accumulating stale names for the whole dev session — the store
     * itself survives reloads via {@link MetricsStoreHolder}. The catalog repopulates from the next
     * sample cycle.
     */
    public void resetCatalog() {
        catalog.clear();
    }
}
