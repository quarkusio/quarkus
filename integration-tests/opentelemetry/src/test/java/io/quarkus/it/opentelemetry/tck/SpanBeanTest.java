package io.quarkus.it.opentelemetry.tck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Same as in the TCK, for early warning and debugging
 */
@QuarkusTest
public class SpanBeanTest {
    @Inject
    private Span injectedSpan;

    @Inject
    private Tracer tracer;

    @Test
    public void spanBeanChange() {
        Span originalSpan = Span.current();
        // Check the injected span reflects the current span initially
        assertEquals(originalSpan.getSpanContext().getSpanId(), injectedSpan.getSpanContext().getSpanId());

        // Create a new span
        Span span1 = tracer.spanBuilder("span1").startSpan();
        // Check we have a real span with a different spanId
        assertNotEquals(originalSpan.getSpanContext().getSpanId(), span1.getSpanContext().getSpanId());

        // The original span should still be "current", so the injected span should still reflect it
        assertEquals(originalSpan.getSpanContext().getSpanId(), injectedSpan.getSpanContext().getSpanId());

        // Make span1 current
        try (Scope s = span1.makeCurrent()) {
            Span current = Span.current();
            // Now the injected span should reflect span1
            assertEquals(span1.getSpanContext().getSpanId(), injectedSpan.getSpanContext().getSpanId());

            // Make a new span
            Span span2 = tracer.spanBuilder("span2").startSpan();
            // Injected span should still reflect span1
            assertEquals(span1.getSpanContext().getSpanId(), injectedSpan.getSpanContext().getSpanId());

            // Make span2 current
            try (Scope s2 = span2.makeCurrent()) {
                // Now the injected span should reflect span2
                assertEquals(span2.getSpanContext().getSpanId(), injectedSpan.getSpanContext().getSpanId());
            } finally {
                span2.end();
            }

            // After closing the scope, span1 is current again and the injected bean should reflect that
            assertEquals(span1.getSpanContext().getSpanId(), injectedSpan.getSpanContext().getSpanId());
        } finally {
            span1.end();
        }

        // After closing the scope, the original span is current again
        assertEquals(originalSpan.getSpanContext().getSpanId(), injectedSpan.getSpanContext().getSpanId());
    }
}
