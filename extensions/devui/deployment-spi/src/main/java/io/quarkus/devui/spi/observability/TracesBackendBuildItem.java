package io.quarkus.devui.spi.observability;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produced only in Dev mode, by a tracer adapter (e.g. OpenTelemetry) that sends its finished spans as {@code span}
 * {@code io.quarkus.dev.telemetry.TelemetryEvent}s. The core Dev UI then registers the traces store that receives
 * them, the JSON-RPC service and the traces view, and offers it on the Observability dashboard.
 */
public final class TracesBackendBuildItem extends MultiBuildItem {

    private final String source;
    private final String title;

    /**
     * @param source which tracer sends the spans, e.g. {@code otel}
     * @param title the title of the traces card on the dashboard; name the tracer, not just the signal, e.g.
     *        {@code OpenTelemetry Traces}. When several tracers send spans, the card is titled after all their
     *        sources instead.
     */
    public TracesBackendBuildItem(String source, String title) {
        this.source = source;
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }
}
