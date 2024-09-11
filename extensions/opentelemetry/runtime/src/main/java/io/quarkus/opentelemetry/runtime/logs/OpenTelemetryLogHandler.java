package io.quarkus.opentelemetry.runtime.logs;

import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

public class OpenTelemetryLogHandler extends Handler {
    private static final String THROWN_ATTRIBUTE = "thrown";

    @Override
    public void publish(LogRecord record) {
        ArcContainer container = Arc.container();
        if (container == null || !container.instance(OpenTelemetry.class).isAvailable()) {
            // "quarkus-opentelemetry-deployment stopped in Xs" will never be sent.
            return; // evaluate to perform cache of log entries here and replay them later.
        }
        try (InstanceHandle<OpenTelemetry> openTelemetry = container.instance(OpenTelemetry.class)) {
            if (openTelemetry.isAvailable()) {
                LogRecordBuilder logRecordBuilder = openTelemetry.get().getLogsBridge().loggerBuilder(INSTRUMENTATION_NAME)
                        .build().logRecordBuilder()
                        .setSeverity(mapSeverity(record.getLevel()))
                        .setSeverityText(record.getLevel().getName())
                        .setBody(record.getMessage())
                        .setObservedTimestamp(record.getInstant());

                if (record.getThrown() != null) {
                    // render as a standard out string
                    try (StringWriter sw = new StringWriter(1024); PrintWriter pw = new PrintWriter(sw)) {
                        record.getThrown().printStackTrace(pw);
                        sw.flush();
                        logRecordBuilder.setAttribute(AttributeKey.stringKey(THROWN_ATTRIBUTE), sw.toString());
                    } catch (Throwable t) {
                        logRecordBuilder.setAttribute(AttributeKey.stringKey(THROWN_ATTRIBUTE),
                                "Unable to get the stacktrace of the exception");
                    }
                }

                logRecordBuilder.emit();
            }
        }
    }

    private Severity mapSeverity(Level level) {
        if (level.intValue() == Level.SEVERE.intValue()) {
            return Severity.ERROR;
        }
        if (level.intValue() == Level.WARNING.intValue()) {
            return Severity.WARN;
        }
        if (level.intValue() <= Level.INFO.intValue() && level.intValue() >= Level.CONFIG.intValue()) {
            return Severity.INFO;
        }
        if (level.intValue() == Level.FINE.intValue()) {
            return Severity.DEBUG;
        }
        if (level.intValue() <= Level.FINER.intValue()) {
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
