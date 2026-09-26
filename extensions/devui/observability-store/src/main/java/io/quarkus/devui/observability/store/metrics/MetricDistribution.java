package io.quarkus.devui.observability.store.metrics;

/**
 * The extra statistics a distribution-style meter carries beyond its primary value: Micrometer
 * timers and distribution summaries, OpenTelemetry histograms. Without these the Dev UI can only
 * chart the recording <em>count</em>, which makes a card named after a latency meter plot
 * throughput instead of duration.
 * <p>
 * {@code total} and {@code bucketCounts} are cumulative since the meter was created; the store
 * differences them so the client sees per-interval means and accumulated bucket counts.
 * {@code max} is whatever recent-window maximum the backend tracks, so it is already an interval
 * figure. Bucket counts are PER bucket, not cumulative across buckets: Micrometer reports
 * cumulative-at-bucket, so its adapter de-cumulates before constructing this.
 *
 * @param total sum of everything recorded, in the sample's {@link MetricSample#unit() unit}
 * @param max largest recent recording, in the same unit
 * @param percentileRanks ranks the backend publishes (e.g. 0.5, 0.95); empty when none are
 *        configured, which is the default - Micrometer only computes these for meters that
 *        enable {@code publishPercentiles}, and OpenTelemetry never does
 * @param percentileValues value per rank, same length and order as {@code percentileRanks}
 * @param bucketBoundaries inclusive upper bounds of the histogram buckets, empty when the meter
 *        has no histogram; there is always one implicit overflow bucket above the last bound
 * @param bucketCounts recordings per bucket, length {@code bucketBoundaries.length + 1}
 */
public record MetricDistribution(
        double total,
        double max,
        double[] percentileRanks,
        double[] percentileValues,
        double[] bucketBoundaries,
        double[] bucketCounts) {

    private static final double[] NONE = new double[0];

    public MetricDistribution {
        // Normalise here so nothing downstream has to null-check six arrays.
        percentileRanks = percentileRanks == null ? NONE : percentileRanks;
        percentileValues = percentileValues == null ? NONE : percentileValues;
        bucketBoundaries = bucketBoundaries == null ? NONE : bucketBoundaries;
        bucketCounts = bucketCounts == null ? NONE : bucketCounts;
    }

    /** A timer or summary with neither percentiles nor a histogram configured: sum and max only. */
    public MetricDistribution(double total, double max) {
        this(total, max, null, null, null, null);
    }

    public boolean hasPercentiles() {
        return percentileRanks.length > 0 && percentileValues.length == percentileRanks.length;
    }

    public boolean hasBuckets() {
        return bucketBoundaries.length > 0 && bucketCounts.length == bucketBoundaries.length + 1;
    }
}
