package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.LateBoundSpanProcessor;
import io.quarkus.test.QuarkusUnitTest;

public class OtlpTraceExporterDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.exporter.otlp.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Instance<LateBoundSpanProcessor> lateBoundSpanProcessorInstance;

    @Inject
    Instance<MetricExporter> metricExporters;

    @Test
    void testOpenTelemetryButNoBatchSpanProcessor() {
        assertNotNull(openTelemetry);
        assertFalse(lateBoundSpanProcessorInstance.isResolvable());
        assertFalse(metricExporters.isResolvable());
    }
}
