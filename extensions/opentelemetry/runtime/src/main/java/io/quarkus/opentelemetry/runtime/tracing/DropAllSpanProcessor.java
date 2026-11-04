package io.quarkus.opentelemetry.runtime.tracing;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * A {@link SpanProcessor} that silently drops every span.
 * <p>
 * This is used when tracing is enabled but no exporter is configured, e.g.
 * {@code quarkus.otel.traces.exporter=none} or {@code quarkus.otel.exporter.otlp.enabled=false}. Spans are still
 * created with valid trace and span ids, so log correlation and context propagation to downstream services keep
 * working, but nothing is exported.
 * <p>
 * An actual empty processor list cannot be used here: since SDK 1.67.0, {@code SdkTracerProvider} hands out no-op
 * tracers when no span processor is registered, which would stop spans from being created at all. The SDK detects
 * that case with an {@code instanceof} check against its own package-private {@code NoopSpanProcessor}, so any
 * processor we own is enough to keep real tracers.
 * <p>
 * To stop creating spans altogether, use {@code quarkus.otel.traces.enabled=false} or, at runtime,
 * {@code quarkus.otel.sdk.disabled=true}.
 */
public final class DropAllSpanProcessor implements SpanProcessor {
    private final SpanProcessor replacedBatchProcessor;

    /**
     * To be used when the OpenTelemetry autoconfiguration never created a span processor, e.g. when
     * {@code otel.traces.exporter} is {@code none}, so there is nothing to shut down.
     */
    public DropAllSpanProcessor() {
        this(SpanProcessor.composite());
    }

    /**
     * To be used when this processor replaces a batch processor that was already created, and whose worker thread
     * must still be shut down with the SDK.
     */
    public DropAllSpanProcessor(SpanProcessor replacedBatchProcessor) {
        this.replacedBatchProcessor = replacedBatchProcessor;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
    }

    @Override
    public boolean isStartRequired() {
        return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {
    }

    @Override
    public boolean isEndRequired() {
        return false;
    }

    @Override
    public CompletableResultCode forceFlush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        // the batch processor we replaced already started its worker thread, it must not be left running
        return replacedBatchProcessor.shutdown();
    }

    @Override
    public String toString() {
        return "DropAllSpanProcessor{" +
                "replacedBatchProcessor=" + replacedBatchProcessor +
                '}';
    }
}
