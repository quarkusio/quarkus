package io.quarkus.micrometer.deployment.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.quarkus.micrometer.deployment.MicrometerProcessor;
import io.quarkus.test.QuarkusExtensionTest;

class RedundantMetricsNoRegistryTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.otel.enabled", "true")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .overrideConfigKey("quarkus.otel.exporter.otlp.enabled", "false")
            // Micrometer stays enabled, but no export registry is active; consequently, no provider items are generated.
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.export.json.enabled", "false")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .withEmptyApplication()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && MicrometerProcessor.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records)
                    .extracting(LogRecord::getMessage)
                    .isEmpty());

    @Inject
    MeterRegistry registry;

    @Test
    void micrometerActiveWithNoRegistryDoesNotWarn() {
        // OTel metrics are on and Micrometer is enabled, but with no export registry there are no provider items,
        // therefore nothing is redundant and no warning fires.
        assertThat(((CompositeMeterRegistry) registry).getRegistries())
                .as("no export registry should be active")
                .isEmpty();
    }
}
