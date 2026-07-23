package io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx.OpenTelemetryVertxTracer.SpanOperation;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.tracing.TracingPolicy;

/**
 * Verifies that {@link InstrumenterVertxTracer} honors the Vert.x {@link TracingPolicy}, in particular
 * {@code PROPAGATE}, which must only create a span when there is a trace to continue.
 */
class InstrumenterVertxTracerTest {

    private static final String VALID_TRACEPARENT = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";

    // shouldStart -> true and start -> the parent context, so whether a span is created depends only on the policy logic.
    @SuppressWarnings("unchecked")
    private static TestTracer tracerThatWouldStart() {
        Instrumenter<String, String> instrumenter = mock(Instrumenter.class);
        when(instrumenter.shouldStart(any(), any())).thenReturn(true);
        when(instrumenter.start(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        return new TestTracer(instrumenter);
    }

    @Test
    void sendRequest_ignore_returnsNull() {
        assertNull(sendRequest(tracerThatWouldStart(), TracingPolicy.IGNORE));
    }

    @Test
    void sendRequest_propagate_withoutActiveTrace_returnsNull() {
        assertNull(sendRequest(tracerThatWouldStart(), TracingPolicy.PROPAGATE));
    }

    @Test
    void sendRequest_propagate_withActiveTrace_startsSpan() {
        try (Scope ignored = Context.root().with(Span.wrap(validSpanContext())).makeCurrent()) {
            assertNotNull(sendRequest(tracerThatWouldStart(), TracingPolicy.PROPAGATE));
        }
    }

    @Test
    void sendRequest_always_withoutActiveTrace_startsSpan() {
        assertNotNull(sendRequest(tracerThatWouldStart(), TracingPolicy.ALWAYS));
    }

    @Test
    void receiveRequest_ignore_returnsNull() {
        assertNull(receiveRequest(tracerThatWouldStart(), TracingPolicy.IGNORE, List.of()));
    }

    @Test
    void receiveRequest_propagate_withoutParent_returnsNull() {
        assertNull(receiveRequest(tracerThatWouldStart(), TracingPolicy.PROPAGATE, List.of()));
    }

    @Test
    void receiveRequest_propagate_withRemoteParentInHeaders_isRecognizedAsParent() {
        // A parent propagated through the incoming headers must be recognized so the span is not dropped
        // (distributed trace continuation). Driving actual span creation needs a real Vert.x context, so we
        // assert the extraction path used by receiveRequest resolves the propagated parent.
        Context extracted = W3CTraceContextPropagator.getInstance().extract(
                Context.root(),
                List.of(Map.entry("traceparent", VALID_TRACEPARENT)),
                InstrumenterVertxTracer.HeadersTextMapGetter.INSTANCE);
        assertTrue(Span.fromContext(extracted).getSpanContext().isValid());
    }

    private static SpanOperation sendRequest(TestTracer tracer, TracingPolicy policy) {
        return tracer.sendRequest(null, SpanKind.RPC, policy, "request", "op",
                (BiConsumer<String, String>) (key, value) -> {
                }, TagExtractor.empty());
    }

    private static SpanOperation receiveRequest(TestTracer tracer, TracingPolicy policy,
            Iterable<Map.Entry<String, String>> headers) {
        return tracer.receiveRequest(null, SpanKind.RPC, policy, "request", "op", headers, TagExtractor.empty());
    }

    private static SpanContext validSpanContext() {
        return SpanContext.create("0af7651916cd43dd8448eb211c80319c", "b7ad6b7169203331",
                TraceFlags.getSampled(), TraceState.getDefault());
    }

    private static final class TestTracer implements InstrumenterVertxTracer<String, String> {
        private final Instrumenter<String, String> instrumenter;

        private TestTracer(Instrumenter<String, String> instrumenter) {
            this.instrumenter = instrumenter;
        }

        @Override
        public <R> boolean canHandle(R request, TagExtractor<R> tagExtractor) {
            return true;
        }

        @Override
        public Instrumenter<String, String> getReceiveRequestInstrumenter() {
            return instrumenter;
        }

        @Override
        public Instrumenter<String, String> getSendResponseInstrumenter() {
            return instrumenter;
        }

        @Override
        public Instrumenter<String, String> getSendRequestInstrumenter() {
            return instrumenter;
        }

        @Override
        public Instrumenter<String, String> getReceiveResponseInstrumenter() {
            return instrumenter;
        }

        @Override
        public TextMapPropagator getPropagator() {
            return W3CTraceContextPropagator.getInstance();
        }
    }
}
