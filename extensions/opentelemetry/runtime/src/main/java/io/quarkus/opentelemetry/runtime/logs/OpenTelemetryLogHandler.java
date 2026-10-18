package io.quarkus.opentelemetry.runtime.logs;

import static io.opentelemetry.semconv.CodeAttributes.CODE_FUNCTION_NAME;
import static io.opentelemetry.semconv.CodeAttributes.CODE_LINE_NUMBER;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE;
import static io.opentelemetry.semconv.incubating.LogIncubatingAttributes.LOG_FILE_PATH;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_ID;
import static io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes.THREAD_NAME;
import static io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig.INSTRUMENTATION_NAME;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.logging.Formatter;
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

    private static final AttributeKey<String> NAMESPACE_ATTRIBUTE_KEY = AttributeKey.stringKey("log.logger.namespace");
    // See: https://github.com/open-telemetry/semantic-conventions/issues/1550
    public static final AttributeKey<String> BRIDGE_NAME = AttributeKey.stringKey("bridge.name");

    private static final int PENDING_RECORDS_LIMIT = 256;

    private volatile OpenTelemetry openTelemetry;
    private final boolean logFileEnabled;
    private final String logFilePath;

    private final Object lock = new Object();

    // Records published before the OpenTelemetry SDK is available; drained on activation.
    // Set to null after activation or close to release the backing array.
    private Deque<ExtLogRecord> pendingRecords = new ArrayDeque<>();

    /**
     * Creates a handler that buffers records (up to a limit) until
     * {@link #activate(OpenTelemetry)} provides the SDK instance. This allows the handler
     * to be installed during early logging setup, before the OpenTelemetry SDK — which
     * requires the CDI container — has been initialized.
     */
    public OpenTelemetryLogHandler() {
        final Config config = ConfigProvider.getConfig();
        this.logFileEnabled = config.getValue("quarkus.log.file.enabled", boolean.class);
        this.logFilePath = this.logFileEnabled ? config.getValue("quarkus.log.file.path", String.class) : null;
    }

    /**
     * Provides the SDK instance and emits any records that were published before it was available.
     */
    void activate(OpenTelemetry openTelemetry) {
        Deque<ExtLogRecord> pendingRecords;
        synchronized (lock) {
            this.openTelemetry = openTelemetry;
            pendingRecords = this.pendingRecords;
            this.pendingRecords = null;
        }
        if (pendingRecords != null) {
            ExtLogRecord record;
            while ((record = pendingRecords.pollFirst()) != null) {
                emit(openTelemetry, record);
            }
        }
    }

    @Override
    protected void doPublish(ExtLogRecord record) {
        OpenTelemetry openTelemetry = this.openTelemetry;
        if (openTelemetry == null) {
            synchronized (lock) {
                openTelemetry = this.openTelemetry;
                if (openTelemetry == null) {
                    Deque<ExtLogRecord> pendingRecords = this.pendingRecords;
                    if (pendingRecords == null) {
                        // closed before activation
                        return;
                    }
                    // The SDK is not available yet: hold on to the record until it is.
                    // Drop new records when full: the window only lasts until runtime init completes.
                    if (pendingRecords.size() < PENDING_RECORDS_LIMIT) {
                        // prepare the record to be formatted on another thread, later
                        record.copyAll();
                        pendingRecords.addLast(record);
                    }
                    return;
                }
            }
        }
        emit(openTelemetry, record);
    }

    private void emit(OpenTelemetry openTelemetry, ExtLogRecord record) {
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
            // Get the message
            final Formatter formatter = getFormatter();
            String logMsg;
            if (formatter != null) {
                logMsg = formatter.format(record);
            } else {
                logMsg = record.getFormattedMessage();
            }
            logRecordBuilder.setBody(logMsg);
        }

        final AttributesBuilder attributes = Attributes.builder();
        attributes.put(CODE_FUNCTION_NAME, record.getSourceClassName() + "." + record.getSourceMethodName());

        attributes.put(CODE_LINE_NUMBER, record.getSourceLineNumber());
        attributes.put(THREAD_NAME, record.getThreadName());
        attributes.put(THREAD_ID, record.getLongThreadID());
        attributes.put(NAMESPACE_ATTRIBUTE_KEY, record.getLoggerClassName());
        attributes.put(BRIDGE_NAME, record.getLoggerName());

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
        if (logFileEnabled && logFilePath != null) {
            attributes.put(LOG_FILE_PATH, logFilePath);
        }

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
        synchronized (lock) {
            pendingRecords = null;
        }
    }
}
