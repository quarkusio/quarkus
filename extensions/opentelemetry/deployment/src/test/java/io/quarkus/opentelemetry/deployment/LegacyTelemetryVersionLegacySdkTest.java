package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.logging.Level;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeSdk;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Internal telemetry version {@code legacy} wires the SDK's self-metrics differently from upstream's declarative
 * configuration, so it must select the legacy engine.
 */
public class LegacyTelemetryVersionLegacySdkTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.otel.experimental.sdk.telemetry.version", "legacy")
            .overrideConfigKey("quarkus.otel.traces.exporter", "none")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")
            .setLogRecordPredicate(record -> QuarkusDeclarativeSdk.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertThat(records)
                    .anySatisfy(record -> assertThat(record.getParameters())
                            .contains("The SDK internal telemetry version is legacy")));

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void legacyEngineIsSelected() {
        // forces the synthetic OpenTelemetry bean
        assertNotNull(openTelemetry.getTracer("test"));
    }
}
