package io.quarkus.devui.spi.observability;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Produced (dev-only) by each active metrics backend adapter (micrometer / otel) to signal
 * that metrics capture is wired. The core {@code MetricsDevUIProcessor} registers the
 * JSON-RPC service, page and the single Observability signal when at least one is present.
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
