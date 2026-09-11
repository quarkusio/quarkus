package io.quarkus.micrometer.deployment.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.quarkus.micrometer.deployment.MicrometerProcessor;
import io.quarkus.test.QuarkusExtensionTest;

class RedundantMetricsTracingOnlyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.otel.enabled", "true")
            // OTel present for tracing, but metrics off -> OPENTELEMETRY_METRICS capability absent.
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.exporter.otlp.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
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
    public void prometheusActiveButNoRedundancyWarning() {
        assertThat(((CompositeMeterRegistry) registry).getRegistries())
                .as("Micrometer's Prometheus registry should be active even though no warning fires")
                .singleElement()
                .isInstanceOf(PrometheusMeterRegistry.class);
    }
}
