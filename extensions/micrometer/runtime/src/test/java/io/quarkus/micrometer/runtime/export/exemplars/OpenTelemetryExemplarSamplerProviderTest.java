package io.quarkus.micrometer.runtime.export.exemplars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;

/**
 * Regression test for <a href="https://github.com/quarkusio/quarkus/issues/55855">#55855</a>:
 * Prometheus exemplar sampling must not pass a null OpenTelemetry {@link Context} into
 * {@link Span#fromContextOrNull(Context)}, which OpenTelemetry 1.62+ reports as API misuse.
 */
public class OpenTelemetryExemplarSamplerProviderTest {

    @Test
    void noCurrentContextDoesNotPassNullToFromContextOrNull() {
        assertNull(QuarkusContextStorage.INSTANCE.current());

        io.prometheus.metrics.tracer.common.SpanContext sampler = new OpenTelemetryExemplarSamplerProvider()
                .exemplarSampler().orElseThrow();

        // Assert the call site itself — do not scrape JUL logs. Log capture is brittle under
        // JBoss LogManager and ApiUsageLogger only emits the detailed misuse text at FINEST.
        try (MockedStatic<Span> span = Mockito.mockStatic(Span.class, Mockito.CALLS_REAL_METHODS)) {
            assertNull(sampler.getCurrentTraceId());
            assertNull(sampler.getCurrentSpanId());
            assertFalse(sampler.isCurrentSpanSampled());
            span.verify(() -> Span.fromContextOrNull(null), never());
        }
    }

    @Test
    void currentSpanIsExposedToPrometheusExemplarSampler() {
        String traceId = "0123456789abcdef0123456789abcdef";
        String spanId = "0123456789abcdef";
        Span span = Span.wrap(SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault()));

        io.prometheus.metrics.tracer.common.SpanContext sampler = new OpenTelemetryExemplarSamplerProvider()
                .exemplarSampler().orElseThrow();

        try (Scope scope = QuarkusContextStorage.INSTANCE.attach(Context.root().with(span))) {
            assertEquals(traceId, sampler.getCurrentTraceId());
            assertEquals(spanId, sampler.getCurrentSpanId());
            assertTrue(sampler.isCurrentSpanSampled());
        }
    }
}
