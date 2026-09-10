package io.quarkus.devui.observability.store.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cheap, no-history catalog of the metrics currently seen at runtime, grouped by the
 * metric-name segment before the first '.'. Backs the Dev UI metric picker. Updated on
 * every sample; holds only current metadata (one entry per name), never a time-series.
 * Pure — no Vert.x; the service turns {@link #snapshot()} into JSON.
 */
public final class MetricCatalog {

    private static final class Entry {
        final String name;
        final String group;
        volatile String type;
        volatile boolean cumulative;
        final java.util.Set<String> seriesKeys = ConcurrentHashMap.newKeySet();
        volatile double lastValue;

        Entry(String name, String group, String type, boolean cumulative) {
            this.name = name;
            this.group = group;
            this.type = type;
            this.cumulative = cumulative;
        }
    }

    private final Map<String, Entry> byName = new ConcurrentHashMap<>();

    public void record(MetricSample s) {
        Entry e = byName.computeIfAbsent(s.name(),
                n -> new Entry(n, group(n), s.type(), s.cumulative()));
        e.type = s.type();
        e.cumulative = s.cumulative();
        e.seriesKeys.add(s.seriesKey());
        e.lastValue = s.value();
    }

    public void clear() {
        byName.clear();
    }

    /** Number of distinct metric names known (meters available to chart). Backs the tile. */
    public int size() {
        return byName.size();
    }

    static String group(String name) {
        int i = name.indexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

    /** Current metadata for every known metric name, ordered by name (stable UI). */
    public List<MetricCatalogEntry> snapshot() {
        List<MetricCatalogEntry> out = new ArrayList<>();
        TreeMap<String, Entry> ordered = new TreeMap<>(byName);
        for (Entry e : ordered.values()) {
            out.add(new MetricCatalogEntry(e.name, e.group, e.type, e.cumulative,
                    e.seriesKeys.size(), e.lastValue));
        }
        return out;
    }
}
