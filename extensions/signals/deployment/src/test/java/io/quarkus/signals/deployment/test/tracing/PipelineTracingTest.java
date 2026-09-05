package io.quarkus.signals.deployment.test.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that the OpenTelemetry trace context is propagated through a multi-stage signal pipeline (see the
 * <em>Pipeline Pattern</em> in the guide), producing a single linear trace across all execution models:
 * blocking -&gt; non-blocking -&gt; virtual thread.
 */
public class PipelineTracingTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(PlaceOrder.class, ValidatedOrder.class, EnrichedOrder.class, ShipmentConfirmation.class,
                            ValidationStage.class, EnrichmentStage.class, ShipmentStage.class,
                            InMemorySpanExporterProducer.class)
                    .addAsResource(new StringAsset("""
                            quarkus.otel.traces.sampler=always_on
                            quarkus.otel.bsp.export.timeout=1s
                            quarkus.otel.bsp.schedule.delay=50
                            """), "application.properties"))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-opentelemetry-deployment", Version.getVersion())));

    private static final AttributeKey<String> SIGNAL_TYPE = AttributeKey.stringKey("signals.signal.type");

    @Inject
    Signal<PlaceOrder> placeOrder;

    @Inject
    InMemorySpanExporter exporter;

    @Inject
    Tracer tracer;

    @BeforeEach
    void reset() {
        exporter.reset();
    }

    @Test
    public void testTraceContextPropagatesThroughPipeline() {
        Span parent = tracer.spanBuilder("test-parent").startSpan();
        SpanContext parentCtx = parent.getSpanContext();
        ShipmentConfirmation confirmation;
        try (Scope scope = parent.makeCurrent()) {
            confirmation = placeOrder.reactive()
                    .request(new PlaceOrder("ORD-1", "Widget", 3), ShipmentConfirmation.class)
                    .ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
        } finally {
            parent.end();
        }
        assertEquals("ORD-1", confirmation.orderId());

        List<SpanData> spans = awaitReceiveSpans(3);
        SpanData stage1 = spanForType(spans, "PlaceOrder");
        SpanData stage2 = spanForType(spans, "ValidatedOrder");
        SpanData stage3 = spanForType(spans, "EnrichedOrder");

        // A single trace spans all stages
        assertEquals(parentCtx.getTraceId(), stage1.getTraceId());
        assertEquals(parentCtx.getTraceId(), stage2.getTraceId());
        assertEquals(parentCtx.getTraceId(), stage3.getTraceId());

        // Linear parent-child chain: parent -> stage1 -> stage2 -> stage3
        assertEquals(parentCtx.getSpanId(), stage1.getParentSpanId());
        assertEquals(stage1.getSpanId(), stage2.getParentSpanId());
        assertEquals(stage2.getSpanId(), stage3.getParentSpanId());
    }

    private static SpanData spanForType(List<SpanData> spans, String simpleTypeName) {
        return spans.stream()
                .filter(s -> {
                    String type = s.getAttributes().get(SIGNAL_TYPE);
                    return type != null && type.endsWith(simpleTypeName);
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("No span for signal type " + simpleTypeName));
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

    // --- Signal types ---

    record PlaceOrder(String orderId, String item, int quantity) {
    }

    record ValidatedOrder(String orderId, String item, int quantity) {
    }

    record EnrichedOrder(String orderId, String item, int quantity, int totalPrice) {
    }

    record ShipmentConfirmation(String orderId, String trackingId) {
    }

    // --- Stages ---

    @Singleton
    public static class ValidationStage {

        @Inject
        Signal<ValidatedOrder> validatedOrder;

        // Blocking signature
        ShipmentConfirmation onPlaceOrder(@Receives PlaceOrder order) {
            return validatedOrder.request(
                    new ValidatedOrder(order.orderId(), order.item(), order.quantity()),
                    ShipmentConfirmation.class);
        }
    }

    @Singleton
    public static class EnrichmentStage {

        @Inject
        Signal<EnrichedOrder> enrichedOrder;

        @NonBlocking
        Uni<ShipmentConfirmation> onValidatedOrder(@Receives ValidatedOrder order) {
            return enrichedOrder.reactive().request(
                    new EnrichedOrder(order.orderId(), order.item(), order.quantity(), order.quantity() * 10),
                    ShipmentConfirmation.class);
        }
    }

    @Singleton
    public static class ShipmentStage {

        @RunOnVirtualThread
        ShipmentConfirmation onEnrichedOrder(@Receives EnrichedOrder order) {
            return new ShipmentConfirmation(order.orderId(), "SHIP-" + order.orderId());
        }
    }
}
