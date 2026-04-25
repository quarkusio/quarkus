package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.logs.NoopLogRecordExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.NoopMetricExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.LateBoundSpanProcessor;
import io.quarkus.test.QuarkusExtensionTest;

public class OtlpCoexistenceTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.experimental.otlp.default.enable", "true")
            .overrideConfigKey("quarkus.otel.traces.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.logs.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .overrideConfigKey("quarkus.otel.logs.enabled", "true");

    @Inject
    Instance<LateBoundSpanProcessor> lateBoundSpanProcessorInstance;

    @Inject
    Instance<MetricExporter> metricExporter;

    @Inject
    Instance<LogRecordExporter> logRecordExporter;

    @Test
    void testTracesCoexist() {
        assertTrue(lateBoundSpanProcessorInstance.isResolvable(),
                "Tracing: LateBoundSpanProcessor should be resolvable");
        assertFalse(lateBoundSpanProcessorInstance.get().isDelegateNull(),
                "Tracing: Delegate should be initialized");
    }

    @Test
    void testMetricsCoexist() {
        assertTrue(metricExporter.isResolvable(),
                "Metrics: MetricExporter should be resolvable");
        assertFalse(metricExporter.get() instanceof NoopMetricExporter,
                "Metrics: Should not be a NoopMetricExporter");
    }

    @Test
    void testLogsCoexist() {
        assertTrue(logRecordExporter.isResolvable(),
                "Logs: LogRecordExporter should be resolvable");
        assertFalse(logRecordExporter.get() instanceof NoopLogRecordExporter,
                "Logs: Should not be a NoopLogRecordExporter");
    }
}
