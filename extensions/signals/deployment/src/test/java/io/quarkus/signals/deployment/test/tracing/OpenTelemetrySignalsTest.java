package io.quarkus.signals.deployment.test.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;
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
                    .addClasses(PingReceivers.class, Ping.class, InMemorySpanExporterProducer.class)
                    .addAsResource(new StringAsset("""
                            quarkus.otel.traces.sampler=always_on
                            quarkus.otel.bsp.export.timeout=1s
                            quarkus.otel.bsp.schedule.delay=50
                            """), "application.properties"))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-opentelemetry-deployment", Version.getVersion())));

    private static final AttributeKey<String> SIGNAL_TYPE = AttributeKey.stringKey("signals.signal.type");
    private static final AttributeKey<String> EMISSION_TYPE = AttributeKey.stringKey("signals.emission.type");

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

        List<SpanData> spans = awaitReceiveSpans(1);
        assertEquals(1, spans.size());
        SpanData child = spans.get(0);
        assertEquals("signal receive", child.getName());
        assertEquals(SpanKind.INTERNAL, child.getKind());
        assertEquals(parentCtx.getTraceId(), child.getTraceId());
        assertEquals(parentCtx.getSpanId(), child.getParentSpanId());
        assertTrue(child.getAttributes().get(SIGNAL_TYPE).endsWith("Ping"));
        assertEquals("REQUEST", child.getAttributes().get(EMISSION_TYPE));
    }

    @Test
    public void testPublishCreatesSiblingSpans() {
        Span parent = tracer.spanBuilder("test-parent").startSpan();
        SpanContext parentCtx = parent.getSpanContext();
        try (Scope scope = parent.makeCurrent()) {
            ping.reactive().publish(new Ping("multi"))
                    .ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
        } finally {
            parent.end();
        }

        // Two receivers match the Ping signal
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
    }

    @Test
    public void testNoActiveTraceNoSpanCreated() {
        // Emitted outside of any active trace - no span must be created for the receiver
        String result = ping.reactive().request(new Ping("hello"), String.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();
        assertEquals("hello", result);

        // Give any (unexpected) span a chance to be exported
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertTrue(receiveSpans().isEmpty(), "No receiver span must be created without an active trace");
    }

    @Test
    public void testSpanIsCurrentOnVirtualThreadReceiver() {
        // A programmatic receiver running on a virtual thread must see the receiver span as the current span
        String[] receiverSpanId = new String[1];
        boolean[] onVirtualThread = new boolean[1];
        var registration = receivers.newReceiver(Pong.class)
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
        } finally {
            registration.unregister();
        }
    }

    private List<SpanData> receiveSpans() {
        return exporter.getFinishedSpanItems().stream()
                .filter(s -> s.getName().equals("signal receive"))
                .toList();
    }

    private List<SpanData> awaitReceiveSpans(int count) {
        Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> receiveSpans().size() >= count);
        return receiveSpans();
    }

    record Ping(String value) {
    }

    record Pong(String value) {
    }

    @Singleton
    public static class PingReceivers {

        String ping(@Receives Ping ping) {
            if ("boom".equals(ping.value())) {
                throw new IllegalStateException("boom");
            }
            return ping.value();
        }

        void observe(@Receives Ping ping) {
            // A second receiver so that publish delivers to two receivers
        }
    }
}
