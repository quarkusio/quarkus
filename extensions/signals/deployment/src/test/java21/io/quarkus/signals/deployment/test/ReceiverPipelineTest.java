package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;

/**
 * Tests a pipeline where three receivers communicate via signals:
 * <ol>
 * <li>Stage 1 (blocking): receives {@link PlaceOrder}, validates it, emits {@link ValidatedOrder}</li>
 * <li>Stage 2 (non-blocking): receives {@link ValidatedOrder}, enriches it, emits {@link EnrichedOrder}</li>
 * <li>Stage 3 (virtual thread): receives {@link EnrichedOrder}, produces the final {@link ShipmentConfirmation}</li>
 * </ol>
 */
public class ReceiverPipelineTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(
                    PlaceOrder.class, ValidatedOrder.class, EnrichedOrder.class, ShipmentConfirmation.class,
                    ValidationReceiver.class, EnrichmentReceiver.class, ShipmentReceiver.class));

    @Inject
    Signal<PlaceOrder> placeOrder;

    @Inject
    ValidationReceiver validationReceiver;

    @Inject
    EnrichmentReceiver enrichmentReceiver;

    @Inject
    ShipmentReceiver shipmentReceiver;

    @BeforeEach
    public void clearLogs() {
        validationReceiver.log.clear();
        enrichmentReceiver.log.clear();
        shipmentReceiver.log.clear();
    }

    @Test
    public void testPipeline() {
        // Kick off the pipeline: PlaceOrder → ValidatedOrder → EnrichedOrder → ShipmentConfirmation
        ShipmentConfirmation confirmation = placeOrder.reactive().request(new PlaceOrder("ORD-1", "Widget", 3),
                ShipmentConfirmation.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertEquals("ORD-1", confirmation.orderId());
        assertEquals("Widget", confirmation.item());
        assertEquals(3, confirmation.quantity());
        // Price computed by enrichment stage: quantity * 10
        assertEquals(30, confirmation.totalPrice());
        assertTrue(confirmation.trackingId().startsWith("SHIP-ORD-1-"));

        // Verify all stages were executed
        assertEquals(List.of("validate:ORD-1"), validationReceiver.log);
        assertEquals(List.of("enrich:ORD-1"), enrichmentReceiver.log);
        assertEquals(List.of("ship:ORD-1"), shipmentReceiver.log);
    }

    @Test
    public void testPipelineMultipleOrders() {
        ShipmentConfirmation c1 = placeOrder
                .reactive().request(new PlaceOrder("A", "Gadget", 2), ShipmentConfirmation.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();
        ShipmentConfirmation c2 = placeOrder
                .reactive().request(new PlaceOrder("B", "Gizmo", 5), ShipmentConfirmation.class)
                .ifNoItem().after(Duration.ofSeconds(5)).fail()
                .await().indefinitely();

        assertEquals("A", c1.orderId());
        assertEquals(20, c1.totalPrice());
        assertEquals("B", c2.orderId());
        assertEquals(50, c2.totalPrice());
    }

    // --- Signal types ---

    record PlaceOrder(String orderId, String item, int quantity) {
    }

    record ValidatedOrder(String orderId, String item, int quantity) {
    }

    record EnrichedOrder(String orderId, String item, int quantity, int totalPrice) {
    }

    record ShipmentConfirmation(String orderId, String item, int quantity, int totalPrice, String trackingId) {
    }

    // --- Receivers ---

    /**
     * Stage 1: blocking receiver that validates the order and forwards it.
     */
    @Singleton
    public static class ValidationReceiver {

        final List<String> log = new CopyOnWriteArrayList<>();

        @Inject
        Signal<ValidatedOrder> validatedOrder;

        // Blocking signature (returns ShipmentConfirmation) → BLOCKING
        ShipmentConfirmation onPlaceOrder(@Receives PlaceOrder order) {
            log.add("validate:" + order.orderId());
            // Forward to the next stage and wait for the final result
            return validatedOrder.request(
                    new ValidatedOrder(order.orderId(), order.item(), order.quantity()),
                    ShipmentConfirmation.class);
        }
    }

    /**
     * Stage 2: non-blocking receiver that enriches the order with pricing.
     */
    @Singleton
    public static class EnrichmentReceiver {

        final List<String> log = new CopyOnWriteArrayList<>();

        @Inject
        Signal<EnrichedOrder> enrichedOrder;

        // Uni return type → NON_BLOCKING
        @NonBlocking
        Uni<ShipmentConfirmation> onValidatedOrder(@Receives ValidatedOrder order) {
            log.add("enrich:" + order.orderId());
            int totalPrice = order.quantity() * 10;
            return enrichedOrder.reactive().request(
                    new EnrichedOrder(order.orderId(), order.item(), order.quantity(), totalPrice),
                    ShipmentConfirmation.class);
        }
    }

    /**
     * Stage 3: virtual thread receiver that creates the shipment confirmation.
     */
    @Singleton
    public static class ShipmentReceiver {

        final List<String> log = new CopyOnWriteArrayList<>();

        @RunOnVirtualThread
        ShipmentConfirmation onEnrichedOrder(@Receives EnrichedOrder order) {
            log.add("ship:" + order.orderId());
            String trackingId = "SHIP-" + order.orderId() + "-" + Thread.currentThread().threadId();
            return new ShipmentConfirmation(
                    order.orderId(), order.item(), order.quantity(), order.totalPrice(), trackingId);
        }
    }
}
