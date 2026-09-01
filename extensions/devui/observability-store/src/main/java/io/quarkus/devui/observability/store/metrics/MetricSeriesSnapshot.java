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
        long[] timestamps,
        double[] values) {
}
