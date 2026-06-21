package io.quarkus.opentelemetry.runtime.exporter.otlp.tracing;

import java.util.Collection;

import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.exporter.internal.otlp.traces.SpanReusableDataMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public final class VertxHttpSpanExporter implements SpanExporter {

    private final HttpExporter delegate;
    private final SpanReusableDataMarshaler marshaler;

    public VertxHttpSpanExporter(HttpExporter delegate, MemoryMode memoryMode) {
        this.delegate = delegate;
        this.marshaler = new SpanReusableDataMarshaler(memoryMode, delegate::export);
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        return marshaler.export(spans);
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }
}
