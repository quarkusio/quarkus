package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.MockExporterProducer;
import io.quarkus.test.QuarkusExtensionTest;

public class OtlpCoexistenceTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .withApplicationRoot(root -> root.addClass(MockExporterProducer.class))
            .overrideConfigKey("quarkus.otel.experimental.otlp.default.enable", "true")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .overrideConfigKey("quarkus.otel.logs.enabled", "true")
            .overrideConfigKey("quarkus.otel.traces.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.logs.exporter", "cdi");

    @Inject
    BeanManager beanManager;

    @Test
    void testTracesCoexist() {
        var beans = beanManager.getBeans(SpanExporter.class);
        assertEquals(2, beans.size(), "Coexistence failed: Found " + beans.size() + " beans, expected 2");
    }

    @Test
    void testMetricsCoexist() {
        var beans = beanManager.getBeans(MetricExporter.class);
        assertEquals(2, beans.size(), "Metrics: Coexistence failed. Expected 2 exporters, found: " + beans.size());
    }

    @Test
    void testLogsCoexist() {
        var beans = beanManager.getBeans(LogRecordExporter.class);
        assertEquals(2, beans.size(), "Logs: Coexistence failed. Expected 2 exporters, found: " + beans.size());
    }
}
