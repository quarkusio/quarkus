package io.quarkus.devui.observability.store.metrics;

import java.util.Map;

/**
 * One metric time-series (a single name + tag combination). Stores points compactly in
 * parallel primitive ring buffers (16 bytes/point) bounded by a fixed capacity; callers
 * additionally trim by age via {@link #evictOlderThan(long)}. All mutators are
 * synchronized so the capture thread and the snapshot thread are safe. Pure — no Vert.x;
 * exposes a plain-Java {@link #snapshot()} that the service turns into JSON.
 */
final class MetricSeries {

    final String name;
    final Map<String, String> tags;
    final String type;
    final boolean cumulative;
    final String source;

    private final long[] timestamps;
    private final double[] values;
    private int start; // index of the oldest point
    private int count;

    MetricSeries(String name, Map<String, String> tags, String type, boolean cumulative,
            String source, int maxPoints) {
        this.name = name;
        this.tags = tags;
        this.type = type;
        this.cumulative = cumulative;
        this.source = source;
        this.timestamps = new long[maxPoints];
        this.values = new double[maxPoints];
    }

    synchronized void add(long ts, double value) {
        int cap = timestamps.length;
        if (count < cap) {
            int idx = (start + count) % cap;
            timestamps[idx] = ts;
            values[idx] = value;
            count++;
        } else {
            // Full: overwrite the oldest and advance start.
            timestamps[start] = ts;
            values[start] = value;
            start = (start + 1) % cap;
        }
    }

    synchronized void evictOlderThan(long minTsInclusive) {
        int cap = timestamps.length;
        while (count > 0 && timestamps[start] < minTsInclusive) {
            start = (start + 1) % cap;
            count--;
        }
    }

    synchronized int size() {
        return count;
    }

    /** Ordered oldest→newest point copy plus this series' metadata; no Vert.x. */
    synchronized MetricSeriesSnapshot snapshot() {
        int cap = timestamps.length;
        long[] ts = new long[count];
        double[] vals = new double[count];
        for (int i = 0; i < count; i++) {
            int idx = (start + i) % cap;
            ts[i] = timestamps[idx];
            vals[i] = values[idx];
        }
        return new MetricSeriesSnapshot(name, tags, type, cumulative, source, ts, vals);
    }
}
