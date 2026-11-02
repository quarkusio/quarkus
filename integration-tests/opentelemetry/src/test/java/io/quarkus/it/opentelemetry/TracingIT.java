package io.quarkus.it.opentelemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class TracingIT extends TracingTest {

    /**
     * When running native tests the test class is outside the Quarkus application, so the
     * OpenTelemetry instance built by Quarkus is not reachable from here. Only the span context
     * is propagated, so build one directly.
     */
    @Override
    protected Span createParentSpan() {
        IdGenerator idGenerator = IdGenerator.random();
        return Span.wrap(SpanContext.create(
                idGenerator.generateTraceId(),
                idGenerator.generateSpanId(),
                TraceFlags.getSampled(),
                TraceState.getDefault()));
    }

    @Override
    protected TextMapPropagator parentSpanPropagator() {
        return W3CTraceContextPropagator.getInstance();
    }
}
