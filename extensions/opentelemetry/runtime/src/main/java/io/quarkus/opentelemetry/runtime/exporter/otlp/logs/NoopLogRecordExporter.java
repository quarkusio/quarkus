package io.quarkus.opentelemetry.runtime.exporter.otlp.logs;

import java.util.Collection;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

public class NoopLogRecordExporter implements LogRecordExporter {
    public static final NoopLogRecordExporter INSTANCE = new NoopLogRecordExporter();

    private NoopLogRecordExporter() {
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> collection) {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
