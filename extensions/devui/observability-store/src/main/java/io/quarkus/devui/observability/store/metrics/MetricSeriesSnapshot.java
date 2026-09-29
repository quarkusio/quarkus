package io.quarkus.devui.observability.store.metrics;

import java.util.Map;

/**
 * Plain-Java, Vert.x-free view of one series' points + metadata, returned by the store.
 * The JSON-RPC service (in {@code quarkus-devui} runtime) turns this into a JSON section.
 */
public record MetricSeriesSnapshot(
        String name,
        Map<String, String> tags,
        String type,
        boolean cumulative,
        String source,
        String unit,
        long[] timestamps,
        double[] values,
        Distribution distribution) {

    /**
     * The distribution columns of a timer, summary or histogram series; null on the snapshot when
     * the meter is a plain counter or gauge.
     * <p>
     * {@code totals} is parallel to {@link #timestamps()} and still cumulative, so the client
     * derives the mean of an interval the same way it derives a rate: divide the change in total
     * by the change in the (count-valued) primary series. Buckets have no per-point history -
     * they are the accumulated counts since capture started.
     *
     * @param totals cumulative sum of recorded amounts at each point
     * @param maxes recent-window maximum at each point
     * @param percentileRanks the published ranks, empty when the meter publishes none
     * @param percentileValues {@code [rank][point]}, rows parallel to {@code percentileRanks}
     * @param bucketBoundaries histogram bucket upper bounds, empty when there is no histogram
     * @param bucketCounts accumulated recordings per bucket, one longer than the boundaries
     */
    public record Distribution(
            double[] totals,
            double[] maxes,
            double[] percentileRanks,
            double[][] percentileValues,
            double[] bucketBoundaries,
            double[] bucketCounts) {
    }
}
