package io.quarkus.devui.spi.observability;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produced only in Dev mode, by each active metrics backend adapter (micrometer / otel) to signal
 * that metrics capture is wired. The adapter sends its readings as {@code metric}
 * {@code io.quarkus.dev.telemetry.TelemetryEvent}s; the core {@code MetricsDevUIProcessor} registers the
 * store that receives them, the JSON-RPC service and the single Observability signal when at least one
 * backend is present.
 */
public final class MetricsBackendBuildItem extends MultiBuildItem {

    private final String source;

    public MetricsBackendBuildItem(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}
