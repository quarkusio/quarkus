package io.quarkus.signals.deployment.test.tracing;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.List;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.signals.Receivers;
import io.quarkus.signals.Receivers.ExecutionModel;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the OpenTelemetry trace context is propagated from a signal emission to the receiver invocations.
 */
public class OpenTelemetrySignalsTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(PingReceivers.class, Ping.class, Urgent.class, InMemorySpanExporterProducer.class)
                    // always_on: record and export every span so the assertions can find them; the short batch
                    // processor delays make exported spans show up quickly in the in-memory exporter
                    .addAsResource(new StringAsset("""
                            quarkus.otel.traces.sampler=always_on
                            quarkus.otel.bsp.export.timeout=1s
                            quarkus.otel.bsp.schedule.delay=50
                            """), "application.properties"))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-opentelemetry-deployment", Version.getVersion())));

    private static final AttributeKey<String> SIGNAL_TYPE = AttributeKey.stringKey("signals.signal.type");
    private static final AttributeKey<String> EMISSION_TYPE = AttributeKey.stringKey("signals.emission.type");
    private static final AttributeKey<String> RESPONSE_TYPE = AttributeKey.stringKey("signals.response.type");
    private static final AttributeKey<Long> WAIT_MS = AttributeKey.longKey("signals.receiver.wait_ms");
    private static final AttributeKey<String> RECEIVER = AttributeKey.stringKey("signals.receiver");
    private static final AttributeKey<List<String>> QUALIFIERS = AttributeKey.stringArrayKey("signals.qualifiers");
    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

    @Inject
    Signal<Ping> ping;

    @Inject
    Signal<Pong> pong;

    @Inject
    Receivers receivers;

    @Inject
    InMemorySpanExporter exporter;

    @Inject
    Tracer tracer;

    @BeforeEach
    void reset() {
        exporter.reset();
    }

    @Test
    public void testReceiverSpanIsChildOfEmitterSpan() {
        // Setup: emit a request signal while the "test-parent" span is active on the emitting thread.
        // Expected: exactly one receiver span, created as a child of the parent span (same trace, parent span id ==
        // the parent's span id), of kind INTERNAL and carrying the signal/emission/response type, wait time and the
        // declarative receiver name.
        Span parent = tracer.spanBuilder("test-parent").startSpan();
        SpanContext parentCtx = parent.getSpanContext();
        String result;
        try (Scope scope = parent.makeCurrent()) {
            result = ping.reactive().request(new Ping("hello"), String.class)
                    .ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
        } finally {
            parent.end();
        }
        assertEquals("hello", result);

        // Only the "ping" receiver returns a String, so the request resolves to a single receiver -> a single span
        List<SpanData> spans = awaitReceiveSpans(1);
        assertEquals(1, spans.size());
        SpanData child = spans.get(0);
        assertEquals("receive " + Ping.class.getTypeName(), child.getName());
        assertEquals(SpanKind.INTERNAL, child.getKind());
        // Same trace and direct parent-child link with the emitter span
        assertEquals(parentCtx.getTraceId(), child.getTraceId());
        assertEquals(parentCtx.getSpanId(), child.getParentSpanId());
        assertTrue(child.getAttributes().get(SIGNAL_TYPE).endsWith("Ping"));
        assertEquals("REQUEST", child.getAttributes().get(EMISSION_TYPE));
        assertEquals(String.class.getTypeName(), child.getAttributes().get(RESPONSE_TYPE));
        assertNotNull(child.getAttributes().get(WAIT_MS), "The emit-to-receive wait time should be recorded");
        assertTrue(child.getAttributes().get(RECEIVER).endsWith("PingReceivers#ping"),
                "The declarative receiver name should be recorded");
    }

    @Test
    public void testQualifiersRecorded() {
        // Setup: narrow the emission to the @Urgent qualifier via select(...); this reaches only the @Urgent-qualified
        // "urgentPing" receiver (the plain "ping"/"observe" receivers do not match).
        // Expected: the receiver span records the emission qualifiers, with the built-in @Default (added by select on
        // top of the @Urgent qualifier) filtered out, leaving only @Urgent.
        Span parent = tracer.spanBuilder("test-parent").startSpan();
        String result;
        try (Scope scope = parent.makeCurrent()) {
            result = ping.select(Urgent.Literal.INSTANCE)
                    .reactive().request(new Ping("urgent"), String.class)
                    .ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
        } finally {
            parent.end();
        }
        assertEquals("urgent", result);

        List<SpanData> spans = awaitReceiveSpans(1);
        assertEquals(1, spans.size());
        SpanData child = spans.get(0);
        List<String> qualifiers = child.getAttributes().get(QUALIFIERS);
        assertNotNull(qualifiers, "The emission qualifiers should be recorded");
        // Only the user-defined @Urgent qualifier remains; the built-in @Default/@Any are filtered out
        assertEquals(List.of(Urgent.class.getName()), qualifiers);
    }

    @Test
    public void testPublishCreatesSiblingSpans() {
        // Setup: publish (multicast) a Ping while the "test-parent" span is active; both plain receivers
        // ("ping" and "observe") match the unqualified emission.
        // Expected: two sibling receiver spans, each a direct child of the same emitter span (same trace and parent
        // span id) and tagged with the PUBLISH emission type.
        Span parent = tracer.spanBuilder("test-parent").startSpan();
        SpanContext parentCtx = parent.getSpanContext();
        try (Scope scope = parent.makeCurrent()) {
            ping.reactive().publish(new Ping("multi"))
                    .ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
        } finally {
            parent.end();
        }

        // The two matching receivers -> one span each, both siblings under the emitter span
        List<SpanData> spans = awaitReceiveSpans(2);
        assertEquals(2, spans.size());
        for (SpanData child : spans) {
            assertEquals(parentCtx.getTraceId(), child.getTraceId());
            assertEquals(parentCtx.getSpanId(), child.getParentSpanId());
            assertEquals("PUBLISH", child.getAttributes().get(EMISSION_TYPE));
        }
    }

    @Test
    public void testFailureRecordsErrorStatus() {
        // Setup: the "ping" receiver throws IllegalStateException for the "boom" payload.
        // Expected: the receiver span reflects the failure - ERROR status, the exception recorded as a span event, and
        // the error.type attribute set to the exception class name.
        Span parent = tracer.spanBuilder("test-parent").startSpan();
        try (Scope scope = parent.makeCurrent()) {
            assertThrows(RuntimeException.class, () -> ping.reactive().request(new Ping("boom"), String.class)
                    .ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely());
        } finally {
            parent.end();
        }

        List<SpanData> spans = awaitReceiveSpans(1);
        SpanData child = spans.get(0);
        assertEquals(StatusCode.ERROR, child.getStatus().getStatusCode());
        assertEquals(1, child.getEvents().size(), "The exception should be recorded as a span event");
        assertEquals(IllegalStateException.class.getName(), child.getAttributes().get(ERROR_TYPE));
    }

    @Test
    public void testNoActiveTraceCreatesRootSpan() {
        // Setup: emit a request with no active trace on the emitting thread (no parent span is made current), so no
        // trace context is propagated to the receiver.
        // Expected: the receiver still creates a span, but as a new root span (a brand new trace with no parent), and
        // it still records the emit-to-receive wait time.
        String result = ping.reactive().request(new Ping("hello"), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();
        assertEquals("hello", result);

        List<SpanData> spans = awaitReceiveSpans(1);
        assertEquals(1, spans.size());
        SpanData child = spans.get(0);
        assertEquals("receive " + Ping.class.getTypeName(), child.getName());
        // No propagated parent context -> the span is the root of a new trace
        assertFalse(child.getParentSpanContext().isValid(), "The receiver span must be a root span (no parent)");
        assertTrue(child.getAttributes().get(SIGNAL_TYPE).endsWith("Ping"));
        assertNotNull(child.getAttributes().get(WAIT_MS), "The emit-to-receive wait time should be recorded");
    }

    @Test
    public void testSpanIsCurrentOnVirtualThreadReceiver() {
        // Setup: register a named programmatic receiver that runs on a virtual thread and captures, from inside the
        // callback, whether it runs on a virtual thread and which span is current at that point.
        // Expected: the callback runs on a virtual thread and sees the receiver span as the current span (so nested
        // instrumentation would be correlated), and the span carries the programmatic receiver name.
        String[] receiverSpanId = new String[1];
        boolean[] onVirtualThread = new boolean[1];
        var registration = receivers.newReceiver(Pong.class)
                .setName("pong-receiver")
                .setExecutionModel(ExecutionModel.VIRTUAL_THREAD)
                .notify(ctx -> {
                    onVirtualThread[0] = Thread.currentThread().isVirtual();
                    receiverSpanId[0] = Span.current().getSpanContext().getSpanId();
                });
        try {
            Span parent = tracer.spanBuilder("test-parent").startSpan();
            try (Scope scope = parent.makeCurrent()) {
                pong.reactive().send(new Pong("vt"))
                        .ifNoItem().after(Duration.ofSeconds(5)).fail()
                        .await().indefinitely();
            } finally {
                parent.end();
            }

            List<SpanData> spans = awaitReceiveSpans(1);
            SpanData child = spans.get(0);
            assertTrue(onVirtualThread[0], "Receiver should be executed on a virtual thread");
            assertEquals(child.getSpanId(), receiverSpanId[0],
                    "The receiver span must be the current span inside the virtual-thread receiver");
            assertEquals("pong-receiver", child.getAttributes().get(RECEIVER),
                    "The programmatic receiver name should be recorded");
        } finally {
            registration.unregister();
        }
    }

    // Receiver spans are identified by the "signals.signal.type" attribute; this excludes the "test-parent" span and
    // any other unrelated spans that the exporter may have collected
    private List<SpanData> receiveSpans() {
        return exporter.getFinishedSpanItems().stream()
                .filter(s -> s.getAttributes().get(SIGNAL_TYPE) != null)
                .toList();
    }

    // Spans are exported asynchronously by the batch span processor, so wait until at least the expected count is there
    private List<SpanData> awaitReceiveSpans(int count) {
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> receiveSpans().size() >= count);
        return receiveSpans();
    }

    record Ping(String value) {
    }

    record Pong(String value) {
    }

    @Qualifier
    @Target({ FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    public @interface Urgent {

        final class Literal extends AnnotationLiteral<Urgent> implements Urgent {
            public static final Literal INSTANCE = new Literal();
            private static final long serialVersionUID = 1L;
        }
    }

    @Singleton
    public static class PingReceivers {

        // The unqualified request receiver: returns a String (so request(..., String.class) resolves to it) and throws
        // for the "boom" payload to exercise the failure path
        String ping(@Receives Ping ping) {
            if ("boom".equals(ping.value())) {
                throw new IllegalStateException("boom");
            }
            return ping.value();
        }

        // A second unqualified receiver so that a publish (multicast) delivers to two receivers
        void observe(@Receives Ping ping) {
        }

        // A @Urgent-qualified receiver, only reached when the emission is narrowed with select(@Urgent)
        String urgentPing(@Receives @Urgent Ping ping) {
            return ping.value();
        }
    }
}
