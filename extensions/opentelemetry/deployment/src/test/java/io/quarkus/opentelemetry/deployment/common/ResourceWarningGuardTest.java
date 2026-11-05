package io.quarkus.opentelemetry.deployment.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

public class ResourceWarningGuardTest {

    @Test
    void matchesResourceBuilderWarnings() {
        // no test in the suite expects the guard to fire, so a predicate that silently stopped
        // matching would leave every guarded test green
        Predicate<LogRecord> predicate = ResourceWarningGuard.predicate();

        assertThat(predicate.test(record(Level.WARNING, "io.opentelemetry.sdk.resources.ResourceBuilder",
                "Removing entity association [service]"))).isTrue();
        assertThat(predicate.test(record(Level.WARNING, "io.opentelemetry.sdk.autoconfigure.EnvironmentResource",
                "Illegal entity declaration"))).isTrue();
        assertThat(predicate.test(record(Level.INFO, "io.opentelemetry.sdk.resources.ResourceBuilder",
                "Removing entity association [service]"))).isFalse();
        assertThat(predicate.test(record(Level.WARNING, "io.opentelemetry.sdk.trace.SdkTracerProvider",
                "Something else"))).isFalse();
        assertThat(predicate.test(record(Level.WARNING, null, "No logger name"))).isFalse();
    }

    @Test
    void assertionReportsEveryCapturedWarning() {
        List<LogRecord> records = List.of(
                record(Level.WARNING, "io.opentelemetry.sdk.resources.ResourceBuilder",
                        "Removing entity association [service]"),
                record(Level.WARNING, "io.opentelemetry.sdk.resources.ResourceBuilder",
                        "Removing entity association [service.instance]"));

        assertThatThrownBy(() -> ResourceWarningGuard.assertion().accept(records))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("logged 2 warning(s)")
                .hasMessageContaining("Removing entity association [service]")
                .hasMessageContaining("Removing entity association [service.instance]");
    }

    private static LogRecord record(Level level, String loggerName, String message) {
        LogRecord record = new LogRecord(level, message);
        record.setLoggerName(loggerName);
        return record;
    }
}
