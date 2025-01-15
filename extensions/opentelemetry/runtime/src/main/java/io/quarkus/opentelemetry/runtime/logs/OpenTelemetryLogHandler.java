package io.quarkus.opentelemetry.runtime.logs;

import static io.opentelemetry.semconv.ExceptionAttributes.*;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_LINENO;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_NAMESPACE;
import static io.opentelemetry.semconv.incubating.LogIncubatingAttributes.LOG_FILE_PATH;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.*;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;

public class OpenTelemetryLogHandler extends ExtHandler {

    private final OpenTelemetry openTelemetry;

    public OpenTelemetryLogHandler(final OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    protected void doPublish(ExtLogRecord record) {
        if (openTelemetry == null) {
            return; // might happen at shutdown
        }
        final LogRecordBuilder logRecordBuilder = openTelemetry.getLogsBridge()
                .loggerBuilder(INSTRUMENTATION_NAME)
                .build().logRecordBuilder()
                .setTimestamp(Instant.now())
                .setObservedTimestamp(record.getInstant());

        if (record.getLevel() != null) {
            logRecordBuilder.setSeverity(mapSeverity(record.getLevel()))
                    .setSeverityText(record.getLevel().getName());
        }

        if (record.getMessage() != null) {
            logRecordBuilder.setBody(record.getMessage());
        }

        final AttributesBuilder attributes = Attributes.builder();
        attributes.put(CODE_NAMESPACE, record.getSourceClassName());
        attributes.put(CODE_FUNCTION, record.getSourceMethodName());

        attributes.put(CODE_LINENO, record.getSourceLineNumber());
        attributes.put(THREAD_NAME, record.getThreadName());
        attributes.put(THREAD_ID, record.getLongThreadID());
        attributes.put(AttributeKey.stringKey("log.logger.namespace"),
                record.getLoggerClassName());

        final Map<String, String> mdcCopy = record.getMdcCopy();
        if (mdcCopy != null) {
            mdcCopy.forEach((k, v) -> {
                // ignore duplicated span data already in the MDC
                if (!k.equalsIgnoreCase("spanid") &&
                        !k.equalsIgnoreCase("traceid") &&
                        !k.equalsIgnoreCase("sampled")) {
                    attributes.put(AttributeKey.stringKey(k), v);
                }
            });
        }

        if (record.getThrown() != null) {
            // render as a standard out string
            // TODO make bytes configurable
            try (StringWriter sw = new StringWriter(1024); PrintWriter pw = new PrintWriter(sw)) {
                record.getThrown().printStackTrace(pw);
                sw.flush();
                attributes.put(EXCEPTION_STACKTRACE, sw.toString());
            } catch (Throwable t) {
                attributes.put(EXCEPTION_STACKTRACE,
                        "Unable to get the stacktrace of the exception");
            }
            attributes.put(EXCEPTION_TYPE, record.getThrown().getClass().getName());
            attributes.put(EXCEPTION_MESSAGE, record.getThrown().getMessage());
        }

        // required by spec
        final Config config = ConfigProvider.getConfig();
        config.getOptionalValue("quarkus.log.file.enable", Boolean.class).ifPresent(enable -> {
            Optional<String> filePath = config.getOptionalValue("quarkus.log.file.path", String.class);
            if (enable.equals(Boolean.TRUE) && filePath.isPresent()) {
                attributes.put(LOG_FILE_PATH, filePath.get());
            }
        });

        logRecordBuilder.setAllAttributes(attributes.build());
        logRecordBuilder.emit();
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
