package io.quarkus.opentelemetry.runtime.exporter.otlp.logs;

import java.util.Collection;
import java.util.List;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

/**
 * A {@link LogRecordExporter} that delegates to multiple {@link LogRecordExporter} instances,
 * exporting to all of them. Used to allow the default OTLP exporter to coexist with
 * user-defined exporters registered via CDI.
 */
public final class CompositeLogRecordExporter implements LogRecordExporter {

    private final List<LogRecordExporter> delegates;

    private CompositeLogRecordExporter(List<LogRecordExporter> delegates) {
        this.delegates = delegates;
    }

    public static LogRecordExporter of(Collection<LogRecordExporter> exporters) {
        List<LogRecordExporter> delegates = List.copyOf(exporters);
        if (delegates.isEmpty()) {
            return NoopLogRecordExporter.INSTANCE;
        }
        if (delegates.size() == 1) {
            return delegates.get(0);
        }
        return new CompositeLogRecordExporter(delegates);
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        List<CompletableResultCode> results = delegates.stream()
                .map(delegate -> delegate.export(logs))
                .toList();
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode flush() {
        List<CompletableResultCode> results = delegates.stream()
                .map(LogRecordExporter::flush)
                .toList();
        return CompletableResultCode.ofAll(results);
    }

    @Override
    public CompletableResultCode shutdown() {
        List<CompletableResultCode> results = delegates.stream()
                .map(LogRecordExporter::shutdown)
                .toList();
        return CompletableResultCode.ofAll(results);
    }
}
