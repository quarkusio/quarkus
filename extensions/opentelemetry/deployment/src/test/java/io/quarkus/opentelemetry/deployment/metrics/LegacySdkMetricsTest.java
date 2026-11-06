package io.quarkus.opentelemetry.deployment.metrics;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Metrics must keep flowing when a deprecated {@link AutoConfiguredOpenTelemetrySdkBuilderCustomizer} engages the
 * legacy autoconfigure engine, including that engine's replacement of the periodic metric reader with the
 * managed-executor one. Deleted with the deprecated SPI.
 */
public class LegacySdkMetricsTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(LegacyEngineCustomizer.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset(
                                    """
                                            quarkus.otel.traces.exporter=none
                                            quarkus.otel.logs.exporter=none
                                            quarkus.otel.metrics.exporter=in-memory
                                            quarkus.otel.metric.export.interval=300ms
                                            quarkus.datasource.devservices.enabled=false
                                            """),
                                    "application.properties"));

    @Inject
    Meter meter;
    @Inject
    InMemoryMetricExporter metricExporter;

    @Test
    void metricsAreExportedOnTheLegacyEngine() {
        meter.counterBuilder("legacy-engine-counter").build().add(5);
        metricExporter.assertCountAtLeast("legacy-engine-counter", null, 1);
    }

    @SuppressWarnings("removal")
    @Singleton
    public static class LegacyEngineCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {
        @Override
        public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
            // Present only so the application initializes through the legacy autoconfigure engine.
        }
    }
}
