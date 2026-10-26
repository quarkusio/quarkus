package io.quarkus.opentelemetry.deployment.traces;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.runtime.tracing.DropAllSpanProcessor;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The default {@code cdi} traces exporter has no {@code SpanExporter} beans when the OTLP exporter is disabled. The
 * batch processor around the no-op exporter is replaced by one that drops every span, so no batch worker thread is
 * started and spans keep valid ids.
 */
public class CdiTracesExporterWithoutBeansTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(TestUtil.class))
            .overrideConfigKey("quarkus.otel.exporter.otlp.enabled", "false")
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .overrideConfigKey("quarkus.otel.logs.enabled", "false")
            .overrideConfigKey("quarkus.datasource.devservices.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void activeProcessorDropsEverything() throws Exception {
        assertInstanceOf(DropAllSpanProcessor.class, TestUtil.getActiveSpanProcessor(openTelemetry));
    }

    @Test
    void spansAreStillCreatedWithValidIds() {
        Span span = openTelemetry.getTracer("test").spanBuilder("not-exported").startSpan();
        try {
            assertTrue(span.getSpanContext().isValid());
        } finally {
            span.end();
        }
    }
}
