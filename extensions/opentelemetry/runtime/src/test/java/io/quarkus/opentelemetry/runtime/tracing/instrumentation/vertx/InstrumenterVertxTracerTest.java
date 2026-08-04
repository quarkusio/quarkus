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
 * Vert.x reports {@code PROPAGATE} both when the caller asked for it and when nothing was configured, so
 * {@link InstrumenterVertxTracer} resolves it through {@link InstrumenterVertxTracer#getDefaultTracingPolicy()}:
 * {@code ALWAYS} by default, {@code PROPAGATE} for instrumentations that should only record inside an existing
 * trace.
 */
class InstrumenterVertxTracerTest {

    private static final String VALID_TRACEPARENT = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";

    /**
     * A tracer that only records inside an existing trace, like the event bus tracer.
     */
    private static TestTracer propagatingTracer() {
        return new TestTracer(instrumenterThatWouldStart(), TracingPolicy.PROPAGATE);
    }

    /**
     * A tracer that keeps the {@code ALWAYS} default, like the HTTP/gRPC/SQL/Redis client tracers.
     */
    private static TestTracer defaultTracer() {
        return new TestTracer(instrumenterThatWouldStart(), null);
    }

    // shouldStart -> true and start -> the parent context, so whether a span is created depends only on the policy logic.
    @SuppressWarnings("unchecked")
    private static Instrumenter<String, String> instrumenterThatWouldStart() {
        Instrumenter<String, String> instrumenter = mock(Instrumenter.class);
        when(instrumenter.shouldStart(any(), any())).thenReturn(true);
        when(instrumenter.start(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        return instrumenter;
    }

    @Test
    void sendRequest_ignore_returnsNull() {
        assertNull(sendRequest(propagatingTracer(), TracingPolicy.IGNORE));
    }

    @Test
    void sendRequest_ignore_withDefaultPolicy_returnsNull() {
        assertNull(sendRequest(defaultTracer(), TracingPolicy.IGNORE));
    }

    @Test
    void sendRequest_propagate_withoutActiveTrace_returnsNull() {
        assertNull(sendRequest(propagatingTracer(), TracingPolicy.PROPAGATE));
    }

    @Test
    void sendRequest_propagate_withActiveTrace_startsSpan() {
        try (Scope ignored = Context.root().with(Span.wrap(validSpanContext())).makeCurrent()) {
            assertNotNull(sendRequest(propagatingTracer(), TracingPolicy.PROPAGATE));
        }
    }

    @Test
    void sendRequest_always_withoutActiveTrace_startsSpan() {
        assertNotNull(sendRequest(propagatingTracer(), TracingPolicy.ALWAYS));
    }

    /**
     * The default policy is {@code ALWAYS}, so a client call made outside a trace still starts one even though
     * Vert.x reports {@code PROPAGATE}.
     */
    @Test
    void sendRequest_propagate_withDefaultPolicy_startsSpan() {
        assertNotNull(sendRequest(defaultTracer(), TracingPolicy.PROPAGATE));
    }

    @Test
    void receiveRequest_ignore_returnsNull() {
        assertNull(receiveRequest(propagatingTracer(), TracingPolicy.IGNORE, List.of()));
    }

    @Test
    void receiveRequest_propagate_withoutParent_returnsNull() {
        assertNull(receiveRequest(propagatingTracer(), TracingPolicy.PROPAGATE, List.of()));
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
        private final TracingPolicy defaultPolicy;

        /**
         * @param defaultPolicy the policy to declare, or {@code null} to keep the interface default
         */
        private TestTracer(Instrumenter<String, String> instrumenter, TracingPolicy defaultPolicy) {
            this.instrumenter = instrumenter;
            this.defaultPolicy = defaultPolicy;
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

        @Override
        public TracingPolicy getDefaultTracingPolicy() {
            return defaultPolicy == null
                    ? InstrumenterVertxTracer.super.getDefaultTracingPolicy()
                    : defaultPolicy;
        }
    }
}
