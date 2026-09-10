package io.quarkus.devui.observability.store.metrics;

/**
 * Plain-Java catalog row for one metric name. The service groups these by {@code group}
 * and shapes them into the picker JSON.
 */
public record MetricCatalogEntry(
        String name,
        String group,
        String type,
        boolean cumulative,
        int seriesCount,
        double lastValue) {
}
