package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public final class SimpleSpanProcessorWithBatchShutdown implements SpanProcessor {
    private final SpanProcessor delegate;
    private final SpanProcessor replacedBatchProcessor;

    public SimpleSpanProcessorWithBatchShutdown(SpanExporter spanExporter, SpanProcessor replacedBatchProcessor) {
        this.delegate = SimpleSpanProcessor.create(spanExporter);
        this.replacedBatchProcessor = replacedBatchProcessor;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        delegate.onStart(parentContext, span);
    }

    @Override
    public boolean isStartRequired() {
        return delegate.isStartRequired();
    }

    @Override
    public void onEnd(ReadableSpan span) {
        delegate.onEnd(span);
    }

    @Override
    public boolean isEndRequired() {
        return delegate.isEndRequired();
    }

    @Override
    public CompletableResultCode forceFlush() {
        return delegate.forceFlush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofAll(List.of(
                delegate.shutdown(),
                replacedBatchProcessor.shutdown()));
    }

    @Override
    public String toString() {
        return "SimpleSpanProcessorWithBatchShutdown{" +
                "delegate=" + delegate +
                ", replacedBatchProcessor=" + replacedBatchProcessor +
                '}';
    }
}
