package io.quarkus.opentelemetry.runtime.exporter.otlp.logs;

import java.util.Collection;

import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.exporter.internal.otlp.logs.LogReusableDataMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

public class VertxHttpLogRecordExporter implements LogRecordExporter {

    private final HttpExporter delegate;
    private final LogReusableDataMarshaler marshaler;

    public VertxHttpLogRecordExporter(HttpExporter delegate, MemoryMode memoryMode) {
        this.delegate = delegate;
        this.marshaler = new LogReusableDataMarshaler(memoryMode, delegate::export);
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        return marshaler.export(logs);
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
