package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.test.QuarkusUnitTest;

public class OtlpTraceExporterDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.exporter.otlp.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Instance<SpanExporter> spanExporterInstance;

    @Inject
    Instance<MetricExporter> metricExporters;

    @Test
    void testOpenTelemetryButNoBatchSpanProcessor() {
        assertNotNull(openTelemetry);
        assertFalse(spanExporterInstance.isResolvable());
        assertFalse(metricExporters.isResolvable());
    }
}
