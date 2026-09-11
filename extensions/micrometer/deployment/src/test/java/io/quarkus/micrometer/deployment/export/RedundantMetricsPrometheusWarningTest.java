package io.quarkus.micrometer.deployment.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.logging.Level;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.quarkus.micrometer.deployment.MicrometerProcessor;
import io.quarkus.test.QuarkusExtensionTest;

class RedundantMetricsPrometheusWarningTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.otel.enabled", "true")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .overrideConfigKey("quarkus.otel.exporter.otlp.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .withEmptyApplication()
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue()
                    && MicrometerProcessor.class.getName().equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records)
                    .hasSize(1)
                    .allSatisfy(record -> assertThat(record.getMessage())
                            .startsWith("Redundant")
                            .contains("Micrometer (registries:", "and OpenTelemetry")));

    @Inject
    MeterRegistry registry;

    @Test
    void redundantMicrometerRegistryIsPrometheus() {
        final Set<MeterRegistry> subRegistries = ((CompositeMeterRegistry) registry).getRegistries();
        assertThat(subRegistries)
                .as("the only composite child should be the Prometheus registry the warning names")
                .singleElement()
                .isInstanceOf(PrometheusMeterRegistry.class);
    }
}
