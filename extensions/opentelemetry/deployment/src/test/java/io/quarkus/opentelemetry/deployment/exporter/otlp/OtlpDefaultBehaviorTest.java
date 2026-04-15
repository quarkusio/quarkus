package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.LateBoundSpanProcessor;
import io.quarkus.test.QuarkusExtensionTest;

public class OtlpDefaultBehaviorTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(MockExporterProducer.class))
            .overrideConfigKey("quarkus.otel.experimental.otlp.default.enable", "false")
            .overrideConfigKey("quarkus.otel.traces.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.logs.exporter", "cdi")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "true")
            .overrideConfigKey("quarkus.otel.logs.enabled", "true");

    @Inject
    Instance<LateBoundSpanProcessor> lateBoundSpanProcessorInstance;

    @Inject
    Instance<MetricExporter> metricExporterInstance;

    @Inject
    Instance<LogRecordExporter> logRecordExporterInstance;

    @Test
    void testTracesOtlpDisabledByDefault() {
        assertFalse(lateBoundSpanProcessorInstance.isResolvable(),
                "Traces: OTLP should be disabled when a custom SpanExporter exists");
    }

    @Test
    void testMetricsOtlpDisabledByDefault() {
        assertEquals(1, countBeans(metricExporterInstance),
                "Metrics: Only the user-defined bean should exist");
    }

    @Test
    void testLogsOtlpDisabledByDefault() {
        assertEquals(1, countBeans(logRecordExporterInstance),
                "Logs: Only the user-defined bean should exist");
    }

    private int countBeans(Instance<?> instance) {
        int count = 0;
        for (Object ignored : instance) {
            count++;
        }
        return count;
    }

    public static class MockExporterProducer {
        @Produces
        @Singleton
        @jakarta.enterprise.inject.Typed(io.opentelemetry.sdk.trace.export.SpanExporter.class)
        public io.opentelemetry.sdk.trace.export.SpanExporter mockSpanExporter() {
            return io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter.create();
        }

        @Produces
        @Singleton
        @jakarta.enterprise.inject.Typed(io.opentelemetry.sdk.metrics.export.MetricExporter.class)
        public io.opentelemetry.sdk.metrics.export.MetricExporter mockMetricExporter() {
            return io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter.create();
        }

        @Produces
        @Singleton
        @jakarta.enterprise.inject.Typed(io.opentelemetry.sdk.logs.export.LogRecordExporter.class)
        public io.opentelemetry.sdk.logs.export.LogRecordExporter mockLogExporter() {
            return io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter.create();
        }
    }
}
