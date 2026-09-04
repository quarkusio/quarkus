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
 * @param value the primary statistic for this sample
 * @param timestampMillis capture time (epoch millis)
 * @param source "micrometer" or "otel" — part of the series key (keeps same-name series
 *        from different backends separate) and shown in tooltips
 */
public record MetricSample(
        String name,
        Map<String, String> tags,
        String type,
        boolean cumulative,
        double value,
        long timestampMillis,
        String source) {

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
