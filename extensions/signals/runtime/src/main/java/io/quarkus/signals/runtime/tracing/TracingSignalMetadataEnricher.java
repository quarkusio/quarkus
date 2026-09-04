package io.quarkus.signals.runtime.tracing;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Singleton;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.quarkus.signals.spi.SignalMetadataEnricher;
import io.smallrye.common.annotation.Identifier;

/**
 * Built-in enricher that records the emit timestamp and captures the current OpenTelemetry trace context so that both can
 * be used by the {@link TracingReceiverInterceptor} when a receiver is invoked.
 * <p>
 * The emit timestamp is stored under the {@link TracingSupport#EMIT_NANOS_METADATA_KEY} key and is always recorded, even
 * without an active trace, because the receiver always starts a span (a child span if a trace context was propagated, a
 * new root span otherwise). The trace context is captured only when there is an active span and stored under the
 * {@link TracingSupport#TRACE_CONTEXT_METADATA_KEY} key as an immutable map of W3C Trace Context headers.
 * <p>
 * The enricher runs synchronously on the emitting thread, which is the only place where the caller's active span is
 * available: receivers are dispatched on a separate (duplicated) context where the trace context would otherwise be
 * lost.
 * <p>
 * It is only registered when the OpenTelemetry tracer capability is present. Existing metadata entries are never
 * overridden.
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
        Map<String, Object> metadata = context.signalContext().metadata();
        // Always record the emit time; the receiver reports the emit-to-receive wait time even for a root span
        if (!metadata.containsKey(TracingSupport.EMIT_NANOS_METADATA_KEY)) {
            context.putMetadata(TracingSupport.EMIT_NANOS_METADATA_KEY, System.nanoTime());
        }
        // Propagate the active trace context, if any, so that the receiver span becomes a child of the emitter's span
        if (Span.current().getSpanContext().isValid()
                && !metadata.containsKey(TracingSupport.TRACE_CONTEXT_METADATA_KEY)) {
            Map<String, String> carrier = new HashMap<>();
            openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), carrier,
                    TracingSupport.SETTER);
            context.putMetadata(TracingSupport.TRACE_CONTEXT_METADATA_KEY, Map.copyOf(carrier));
        }
    }
}
