package io.quarkus.opentelemetry.deployment.traces;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.runtime.tracing.DropAllSpanProcessor;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * {@code quarkus.otel.traces.exporter=none} must stop traces from being exported, not stop them from being created.
 * Spans keep valid ids so that log correlation and context propagation to downstream services keep working.
 */
public class TracesExporterNoneTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestUtil.class))
            .overrideConfigKey("quarkus.otel.traces.exporter", "none")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void activeProcessorDropsEverything() throws Exception {
        assertInstanceOf(DropAllSpanProcessor.class, TestUtil.getActiveSpanProcessor(openTelemetry),
                "Tracing is enabled, so a processor must remain registered to keep the SDK tracers real");
    }

    @Test
    void spansAreStillCreatedWithValidIds() {
        Tracer tracer = openTelemetry.getTracer("test");

        Span span = tracer.spanBuilder("no-exporter-span").startSpan();
        try (Scope scope = span.makeCurrent()) {
            SpanContext spanContext = Span.current().getSpanContext();

            assertTrue(spanContext.isValid(), "Span context must be valid so that ids can be correlated and propagated");
            assertNotEquals(SpanContext.getInvalid().getTraceId(), spanContext.getTraceId());
            assertNotEquals(SpanContext.getInvalid().getSpanId(), spanContext.getSpanId());
            assertTrue(span.isRecording(), "The span must be a real recording span, not a no-op one");
        } finally {
            span.end();
        }
    }

    @Test
    void childSpansGetTheirOwnIds() {
        Tracer tracer = openTelemetry.getTracer("test");

        Span parent = tracer.spanBuilder("parent").startSpan();
        try (Scope parentScope = parent.makeCurrent()) {
            Span child = tracer.spanBuilder("child").startSpan();
            try (Scope childScope = child.makeCurrent()) {
                assertNotEquals(parent.getSpanContext().getSpanId(), child.getSpanContext().getSpanId(),
                        "A child span must get its own span id");
            } finally {
                child.end();
            }
        } finally {
            parent.end();
        }
    }
}
