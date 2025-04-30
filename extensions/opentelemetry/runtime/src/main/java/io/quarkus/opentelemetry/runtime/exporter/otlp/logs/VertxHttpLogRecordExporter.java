package io.quarkus.opentelemetry.runtime.exporter.otlp.logs;

import java.util.Collection;

import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

public class VertxHttpLogRecordExporter implements LogRecordExporter {
    private final HttpExporter<LogsRequestMarshaler> delegate;

    public VertxHttpLogRecordExporter(HttpExporter<LogsRequestMarshaler> delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> collection) {
        return delegate.export(LogsRequestMarshaler.create(collection), collection.size());
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
