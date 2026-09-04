package io.quarkus.signals.runtime.tracing;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Singleton;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.quarkus.signals.spi.SignalMetadataEnricher;
import io.smallrye.common.annotation.Identifier;

/**
 * Built-in enricher that captures the current OpenTelemetry trace context and stores it in the signal metadata under the
 * {@link TracingSupport#METADATA_KEY} key (as an immutable map of W3C Trace Context headers), so that it can be restored
 * by the {@link TracingReceiverInterceptor} when a receiver is invoked.
 * <p>
 * The enricher runs synchronously on the emitting thread, which is the only place where the caller's active span is
 * available: receivers are dispatched on a separate (duplicated) context where the trace context would otherwise be
 * lost.
 * <p>
 * It is only registered when the OpenTelemetry tracer capability is present. An existing
 * {@link TracingSupport#METADATA_KEY} metadata entry is never overridden.
 */
@Identifier(TracingSignalMetadataEnricher.ID)
@Singleton
public class TracingSignalMetadataEnricher implements SignalMetadataEnricher {

    public static final String ID = "quarkus.tracing";

    private final OpenTelemetry openTelemetry;

    TracingSignalMetadataEnricher(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void enrich(EnrichmentContext context) {
        Map<String, String> carrier = new HashMap<>();
        openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), carrier, TracingSupport.SETTER);
        if (carrier.isEmpty()) {
            // No active span/baggage, i.e. nothing to propagate
            return;
        }
        // Do not override an existing entry (e.g. explicitly set by the caller)
        if (!context.signalContext().metadata().containsKey(TracingSupport.METADATA_KEY)) {
            context.putMetadata(TracingSupport.METADATA_KEY, Map.copyOf(carrier));
        }
    }
}
