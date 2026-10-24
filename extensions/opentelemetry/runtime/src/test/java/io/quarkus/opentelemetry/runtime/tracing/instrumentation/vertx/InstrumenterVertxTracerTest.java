package io.quarkus.opentelemetry.runtime.tracing.instrumentation.vertx;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static final String TRACE_ID = "0af7651916cd43dd8448eb211c80319c";
    private static final String SPAN_ID = "b7ad6b7169203331";
    private static final String TRACEPARENT = "00-" + TRACE_ID + "-" + SPAN_ID + "-01";

    @Test
    void ignoreIsNeverTraced() {
        assertThat(sendRequest(propagatingTracer(), TracingPolicy.IGNORE)).isNull();
        assertThat(sendRequest(defaultTracer(), TracingPolicy.IGNORE)).isNull();
        assertThat(receiveRequest(propagatingTracer(), TracingPolicy.IGNORE, List.of())).isNull();
        assertThat(receiveRequest(defaultTracer(), TracingPolicy.IGNORE, List.of())).isNull();
    }

    @Test
    void alwaysIsTracedWithoutAnActiveTrace() {
        assertThat(sendRequest(propagatingTracer(), TracingPolicy.ALWAYS)).isNotNull();
        assertThat(receiveRequest(propagatingTracer(), TracingPolicy.ALWAYS, List.of())).isNotNull();
    }

    @Test
    void sendRequestUnderPropagateIsNotTracedWithoutAnActiveTrace() {
        assertThat(sendRequest(propagatingTracer(), TracingPolicy.PROPAGATE)).isNull();
    }

    @Test
    void sendRequestUnderPropagateIsTracedWithinAnActiveTrace() {
        try (Scope ignored = Context.root().with(Span.wrap(remoteSpanContext())).makeCurrent()) {
            assertThat(sendRequest(propagatingTracer(), TracingPolicy.PROPAGATE)).isNotNull();
        }
    }

    @Test
    void receiveRequestUnderPropagateIsNotTracedWithoutAParent() {
        assertThat(receiveRequest(propagatingTracer(), TracingPolicy.PROPAGATE, List.of())).isNull();
    }

    /**
     * A trace propagated through the incoming headers must still be continued, otherwise a distributed trace
     * would be cut at the receiving boundary.
     */
    @Test
    void receiveRequestUnderPropagateIsTracedWithAParentInTheHeaders() {
        assertThat(receiveRequest(propagatingTracer(), TracingPolicy.PROPAGATE,
                List.of(Map.entry("traceparent", TRACEPARENT)))).isNotNull();
    }

    /**
     * The default is {@code ALWAYS}, so the HTTP, gRPC, SQL and Redis client tracers keep starting a span
     * outside a trace even though Vert.x reports {@code PROPAGATE}. Enforcing {@code PROPAGATE} on them would
     * drop the client root spans they are expected to produce.
     */
    @Test
    void defaultTracerUnderPropagateIsStillTracedWithoutAnActiveTrace() {
        assertThat(sendRequest(defaultTracer(), TracingPolicy.PROPAGATE)).isNotNull();
        assertThat(receiveRequest(defaultTracer(), TracingPolicy.PROPAGATE, List.of())).isNotNull();
    }

    /** A tracer that only records inside an existing trace, like the event bus tracer. */
    private static TestTracer propagatingTracer() {
        return new TestTracer(TracingPolicy.PROPAGATE);
    }

    /** A tracer that keeps the {@code ALWAYS} default, like the HTTP, gRPC, SQL and Redis client tracers. */
    private static TestTracer defaultTracer() {
        return new TestTracer(null);
    }

    private static SpanOperation sendRequest(TestTracer tracer, TracingPolicy policy) {
        return tracer.sendRequest(null, SpanKind.RPC, policy, "request", "operation",
                (BiConsumer<String, String>) (key, value) -> {
                }, TagExtractor.empty());
    }

    private static SpanOperation receiveRequest(TestTracer tracer, TracingPolicy policy,
            Iterable<Map.Entry<String, String>> headers) {
        return tracer.receiveRequest(null, SpanKind.RPC, policy, "request", "operation", headers, TagExtractor.empty());
    }

    private static SpanContext remoteSpanContext() {
        return SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
    }

    /**
     * Drives the real policy logic with an instrumenter that would always start a span, so whether a
     * {@link SpanOperation} comes back depends only on the policy decision under test.
     */
    private static final class TestTracer implements InstrumenterVertxTracer<String, String> {

        private final Instrumenter<String, String> instrumenter = instrumenterThatWouldStart();
        private final TracingPolicy defaultPolicy;

        /** @param defaultPolicy the policy to declare, or {@code null} to keep the interface default */
        private TestTracer(TracingPolicy defaultPolicy) {
            this.defaultPolicy = defaultPolicy;
        }

        @SuppressWarnings("unchecked")
        private static Instrumenter<String, String> instrumenterThatWouldStart() {
            Instrumenter<String, String> instrumenter = mock(Instrumenter.class);
            when(instrumenter.shouldStart(any(), any())).thenReturn(true);
            when(instrumenter.start(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
            return instrumenter;
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
