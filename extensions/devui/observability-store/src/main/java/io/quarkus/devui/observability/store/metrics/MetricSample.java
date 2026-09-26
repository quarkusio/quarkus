package io.quarkus.devui.observability.store.metrics;

import java.util.Map;
import java.util.TreeMap;

/**
 * Immutable, backend-agnostic view of one metric measurement captured for the Dev UI.
 * Produced by the Micrometer sampler and the OpenTelemetry in-memory reader alike.
 * Pure data — no Vert.x/JSON here; JSON shaping is done by the JSON-RPC service in the
 * {@code quarkus-devui} runtime (mirrors how {@code SpanRecord.toJson} lives in the OTel
 * runtime, not in the store lib).
 *
 * @param name metric name (e.g. "http.server.requests")
 * @param tags dimension key/values (Micrometer tags / OTel attributes)
 * @param type meter/instrument type label, e.g. GAUGE, COUNTER, TIMER, SUMMARY, LONG_SUM
 * @param cumulative true for monotonic values (counters, timer counts) that the client
 *        renders as a per-interval rate; false for gauges (rendered as-is)
 * @param value the primary statistic for this sample. For a distribution meter (timer, summary,
 *        histogram) this is the RECORDING COUNT, not a duration or a size — the amounts live in
 *        {@code distribution}
 * @param timestampMillis capture time (epoch millis)
 * @param source "micrometer" or "otel" — part of the series key (keeps same-name series
 *        from different backends separate) and shown in tooltips
 * @param unit the base unit of {@code value} (and of the distribution amounts), e.g. "s",
 *        "bytes"; null when the backend does not declare one. Drives the client's axis label
 *        and value formatting
 * @param distribution extra statistics for distribution meters, null for counters and gauges
 */
public record MetricSample(
        String name,
        Map<String, String> tags,
        String type,
        boolean cumulative,
        double value,
        long timestampMillis,
        String source,
        String unit,
        MetricDistribution distribution) {

    /** A meter with a single statistic and no declared unit: counters, gauges. */
    public MetricSample(String name, Map<String, String> tags, String type, boolean cumulative,
            double value, long timestampMillis, String source) {
        this(name, tags, type, cumulative, value, timestampMillis, source, null, null);
    }

    /**
     * This sample with its per-bucket counts replaced. The store accumulates bucket counts across
     * collections, so what goes out on the live stream is the running total the dashboard charts
     * rather than the raw cumulative counts the backend reported.
     */
    public MetricSample withBucketCounts(double[] counts) {
        MetricDistribution d = distribution;
        return new MetricSample(name, tags, type, cumulative, value, timestampMillis, source, unit,
                new MetricDistribution(d.total(), d.max(), d.percentileRanks(), d.percentileValues(),
                        d.bucketBoundaries(), counts));
    }

    /** Stable per-series identity: source + name + tags sorted by key. */
    public String seriesKey() {
        return seriesKey(source, name, tags);
    }

    public static String seriesKey(String source, String name, Map<String, String> tags) {
        StringBuilder sb = new StringBuilder(source).append('|').append(name).append('{');
        if (tags != null && !tags.isEmpty()) {
            // TreeMap gives deterministic key order regardless of insertion order.
            TreeMap<String, String> sorted = new TreeMap<>(tags);
            boolean first = true;
            for (Map.Entry<String, String> e : sorted.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                sb.append(e.getKey()).append('=').append(e.getValue());
                first = false;
            }
        }
        return sb.append('}').toString();
    }
}
