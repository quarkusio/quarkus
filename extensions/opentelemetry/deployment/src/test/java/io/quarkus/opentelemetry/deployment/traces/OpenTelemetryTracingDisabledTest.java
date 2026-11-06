package io.quarkus.opentelemetry.deployment.traces;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.quarkus.test.QuarkusExtensionTest;

public class OpenTelemetryTracingDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.traces.enabled", "false")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void testNoOpenTelemetry() {
        Assertions.assertNotNull(openTelemetry, "A OpenTelemetry instance must be available");
    }

    @Test
    void noSpansAreCreated() {
        Span span = openTelemetry.getTracer("test").spanBuilder("disabled-span").startSpan();
        try {
            Assertions.assertFalse(span.getSpanContext().isValid(),
                    "Disabling tracing must give no-op tracers, unlike quarkus.otel.traces.exporter=none");
            Assertions.assertFalse(span.isRecording());
        } finally {
            span.end();
        }
    }
}
