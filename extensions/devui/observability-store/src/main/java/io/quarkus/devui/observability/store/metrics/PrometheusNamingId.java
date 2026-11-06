package io.quarkus.devui.observability.store.metrics;

/**
 * How a metrics backend's meters are named once exported to Prometheus, as the backend's build step declares
 * it (the second argument of {@code MetricsBackendBuildItem}) and the Dev UI reads it back to export the
 * dashboard for Grafana. Kept here because this module is the one the metrics backends and the Dev UI both
 * depend on.
 */
public final class PrometheusNamingId {

    /** Micrometer's Prometheus registry, scraped from {@code /q/metrics}. */
    public static final String MICROMETER_PROMETHEUS = "micrometer-prometheus";

    /** OTLP, renamed by Prometheus itself when it receives it. */
    public static final String OTLP = "otlp";

    private PrometheusNamingId() {
    }
}
