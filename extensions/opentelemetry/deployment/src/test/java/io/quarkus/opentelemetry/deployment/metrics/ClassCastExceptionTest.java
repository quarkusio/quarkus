package io.quarkus.opentelemetry.deployment.metrics;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class ClassCastExceptionTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                            .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                            .add(new StringAsset(
                                    "quarkus.otel.metrics.enabled=true\n" +
                                            "quarkus.otel.metrics.exporter=in-memory\n" +
                                            "quarkus.otel.metric.export.interval=300ms\n" +
                                            "quarkus.http.limits.max-connections=50\n" +
                                            "quarkus.otel.traces.exporter=none\n" +
                                            "quarkus.otel.logs.exporter=none\n"),
                                    "application.properties"));

    /**
     * ClassCastException when using OpenTelemetry and max-connections property
     * See https://github.com/quarkusio/quarkus/issues/43852
     */
    @Test
    public void testReproducer() {
        // This test is successful if it does not throw an exception
        Assertions.assertTrue(true);
    }
}
