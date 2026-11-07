package io.quarkus.opentelemetry.deployment.common;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Fails a test when the OpenTelemetry SDK logs a warning while building the {@code Resource}.
 * <p>
 * Since SDK 1.67 a {@code Resource} carries resource entities alongside plain attributes
 * and any ordinary attribute write that changes an entity owned value drops the entity association with a warning.
 * Nothing in the test suite used to notice those warnings.
 */
public final class ResourceWarningGuard {

    private static final String RESOURCE_LOGGER_PREFIX = "io.opentelemetry.sdk.resources.";

    /**
     * The SDK parses {@code otel.entities} here, and reports malformed declarations from this logger
     * and from its nested {@code EntityParser}.
     */
    private static final String ENVIRONMENT_RESOURCE_LOGGER = "io.opentelemetry.sdk.autoconfigure.EnvironmentResource";

    private ResourceWarningGuard() {
    }

    /**
     * Applies the guard to a test. Any resource construction warning captured during the deployment
     * fails the test once it ends.
     */
    public static QuarkusExtensionTest apply(QuarkusExtensionTest test) {
        return test.setLogRecordPredicate(predicate()).assertLogRecords(assertion());
    }

    static Predicate<LogRecord> predicate() {
        return record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                && isResourceLogger(record.getLoggerName());
    }

    static Consumer<List<LogRecord>> assertion() {
        return records -> {
            if (!records.isEmpty()) {
                throw new AssertionError("The OpenTelemetry SDK logged " + records.size()
                        + " warning(s) while building the Resource:\n"
                        + records.stream()
                                .map(record -> "  [" + record.getLoggerName() + "] " + record.getMessage())
                                .collect(Collectors.joining("\n")));
            }
        };
    }

    private static boolean isResourceLogger(String loggerName) {
        if (loggerName == null) {
            return false;
        }
        return loggerName.startsWith(RESOURCE_LOGGER_PREFIX)
                || loggerName.startsWith(ENVIRONMENT_RESOURCE_LOGGER);
    }
}
