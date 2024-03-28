package io.quarkus.opentelemetry.runtime.logging;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public class OpenTelemetryLogHandler extends Handler {
    private final Logger openTelemetry;

    public OpenTelemetryLogHandler(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry.getLogsBridge().get("quarkus-log-appender");
    }

    @Override
    public void publish(LogRecord record) {
        openTelemetry.logRecordBuilder()
                .setSeverity(mapSeverity(record.getLevel()))
                .setSeverityText(record.getLevel().getName())
                .setBody(record.getMessage()) // TODO check that we didn't need to format it
                .setObservedTimestamp(record.getInstant())
                // TODO add attributes
                .emit();
    }

    private Severity mapSeverity(Level level) {
        if (Level.SEVERE.equals(level)) {
            return Severity.ERROR;
        }
        if (Level.WARNING.equals(level)) {
            return Severity.WARN;
        }
        if (Level.INFO.equals(level) || Level.CONFIG.equals(level)) {
            return Severity.INFO;
        }
        if (Level.FINE.equals(level)) {
            return Severity.DEBUG;
        }
        if (Level.FINER.equals(level) || Level.FINEST.equals(level) || Level.ALL.equals(level)) {
            return Severity.TRACE;
        }
        return Severity.UNDEFINED_SEVERITY_NUMBER;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
