package io.quarkus.opentelemetry.runtime.exporter.otlp.tracing;

import java.util.Collection;

import io.opentelemetry.exporter.internal.grpc.GrpcExporter;
import io.opentelemetry.exporter.internal.otlp.traces.SpanReusableDataMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public final class VertxGrpcSpanExporter implements SpanExporter {

    private final GrpcExporter delegate;
    private final SpanReusableDataMarshaler marshaler;

    public VertxGrpcSpanExporter(GrpcExporter delegate, MemoryMode memoryMode) {
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
