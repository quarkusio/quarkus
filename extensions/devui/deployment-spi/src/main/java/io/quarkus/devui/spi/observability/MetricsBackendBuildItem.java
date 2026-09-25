package io.quarkus.devui.spi.observability;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produced only in Dev ode, by each active metrics backend adapter (micrometer / otel) to signal
 * that metrics capture is wired. The core {@code MetricsDevUIProcessor} registers the
 * JSON-RPC service, page and the single Observability signal when at least one is present.
 */
public final class MetricsBackendBuildItem extends MultiBuildItem {

    private final String source;
    private final String prometheusNaming;

    public MetricsBackendBuildItem(String source) {
        this(source, null);
    }

    /**
     * @param source which backend captures the metrics for the Dev UI, {@code micrometer} or {@code otel}
     * @param prometheusNaming how the same metrics are named once exported to Prometheus, which is a
     *        different question: the Dev UI captures the meter's own name, while a Grafana dashboard has to
     *        use the exported one. {@code micrometer-prometheus}, {@code otlp}, or {@code null} when the
     *        application exports its metrics in some other way.
     */
    public MetricsBackendBuildItem(String source, String prometheusNaming) {
        this.source = source;
        this.prometheusNaming = prometheusNaming;
    }

    public String getSource() {
        return source;
    }

    public String getPrometheusNaming() {
        return prometheusNaming;
    }
}
