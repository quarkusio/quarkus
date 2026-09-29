package io.quarkus.devui.observability.store.metrics;

import java.util.Map;

/**
 * One metric time-series (a single name + tag combination). Stores points compactly in
 * parallel primitive ring buffers (16 bytes/point) bounded by a fixed capacity; callers
 * additionally trim by age via {@link #evictOlderThan(long)}. All mutators are
 * synchronized so the capture thread and the snapshot thread are safe. Pure — no Vert.x;
 * exposes a plain-Java {@link #snapshot()} that the service turns into JSON.
 * <p>
 * Distribution meters (timers, summaries, histograms) carry more than one number per sample, so
 * this keeps extra ring columns alongside the primary values. They are allocated lazily: a
 * counter or gauge series still costs only the two base columns.
 */
final class MetricSeries {

    final String name;
    final Map<String, String> tags;
    final String type;
    final boolean cumulative;
    final String source;
    final String unit;

    private final long[] timestamps;
    private final double[] values;

    // Distribution columns, parallel to the two above and sharing their start/count indexing.
    private double[] totals;
    private double[] maxes;
    private double[] percentileRanks;
    private double[][] percentileValues; // [rank][point]

    // Buckets are charted as one distribution, not as a history, so only the running totals are
    // kept. previousBucketCounts is the baseline the next collection is differenced against.
    private double[] bucketBoundaries;
    private double[] previousBucketCounts;
    private double[] bucketTotals;

    private int start; // index of the oldest point
    private int count;

    MetricSeries(String name, Map<String, String> tags, String type, boolean cumulative,
            String source, String unit, int maxPoints) {
        this.name = name;
        this.tags = tags;
        this.type = type;
        this.cumulative = cumulative;
        this.source = source;
        this.unit = unit;
        this.timestamps = new long[maxPoints];
        this.values = new double[maxPoints];
    }

    /**
     * Appends a point and returns the accumulated per-bucket counts after it, or null when this
     * meter has no histogram. The caller puts them on the streamed sample so a live client sees
     * the same running totals as {@link #snapshot()}.
     */
    synchronized double[] add(long ts, double value, MetricDistribution dist) {
        int cap = timestamps.length;
        int idx;
        if (count < cap) {
            idx = (start + count) % cap;
            count++;
        } else {
            // Full: overwrite the oldest and advance start.
            idx = start;
            start = (start + 1) % cap;
        }
        timestamps[idx] = ts;
        values[idx] = value;

        if (dist == null) {
            return null;
        }
        recordDistribution(idx, dist);
        return bucketTotals == null ? null : bucketTotals.clone();
    }

    private void recordDistribution(int idx, MetricDistribution dist) {
        int cap = timestamps.length;
        if (totals == null) {
            totals = new double[cap];
            maxes = new double[cap];
        }
        totals[idx] = dist.total();
        maxes[idx] = dist.max();

        if (dist.hasPercentiles()) {
            if (percentileRanks == null || percentileRanks.length != dist.percentileRanks().length) {
                // First percentile sample, or the meter was reconfigured: (re)shape the columns.
                percentileRanks = dist.percentileRanks().clone();
                percentileValues = new double[percentileRanks.length][cap];
            }
            for (int r = 0; r < percentileRanks.length; r++) {
                percentileValues[r][idx] = dist.percentileValues()[r];
            }
        }
        if (dist.hasBuckets()) {
            accumulateBuckets(dist);
        }
    }

    /**
     * Bucket counts arrive cumulative since the meter was created, which would include traffic
     * from before the user put this meter on the dashboard. Baseline on the first collection and
     * accumulate the differences from there, so the histogram covers the same period as the line
     * charts next to it. A negative difference means the meter was re-created (a live reload
     * does that), in which case the new value is itself the amount recorded since.
     */
    private void accumulateBuckets(MetricDistribution dist) {
        double[] counts = dist.bucketCounts();
        if (bucketTotals == null || bucketTotals.length != counts.length) {
            bucketBoundaries = dist.bucketBoundaries().clone();
            bucketTotals = new double[counts.length];
            previousBucketCounts = counts.clone();
            return;
        }
        for (int i = 0; i < counts.length; i++) {
            double delta = counts[i] - previousBucketCounts[i];
            bucketTotals[i] += delta < 0 ? counts[i] : delta;
        }
        previousBucketCounts = counts.clone();
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
        return new MetricSeriesSnapshot(name, tags, type, cumulative, source, unit, ts, vals,
                distributionSnapshot());
    }

    private MetricSeriesSnapshot.Distribution distributionSnapshot() {
        if (totals == null) {
            return null;
        }
        int cap = timestamps.length;
        double[] tot = new double[count];
        double[] mx = new double[count];
        int ranks = percentileRanks == null ? 0 : percentileRanks.length;
        double[][] pct = new double[ranks][count];
        for (int i = 0; i < count; i++) {
            int idx = (start + i) % cap;
            tot[i] = totals[idx];
            mx[i] = maxes[idx];
            for (int r = 0; r < ranks; r++) {
                pct[r][i] = percentileValues[r][idx];
            }
        }
        return new MetricSeriesSnapshot.Distribution(tot, mx,
                percentileRanks == null ? new double[0] : percentileRanks.clone(), pct,
                bucketBoundaries == null ? new double[0] : bucketBoundaries.clone(),
                bucketTotals == null ? new double[0] : bucketTotals.clone());
    }
}
