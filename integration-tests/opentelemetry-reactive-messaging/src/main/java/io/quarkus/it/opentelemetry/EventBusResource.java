package io.quarkus.it.opentelemetry;

import static io.quarkus.it.opentelemetry.EventBusConsumers.ALWAYS_ADDRESS;
import static io.quarkus.it.opentelemetry.EventBusConsumers.PROPAGATE_ADDRESS;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.tracing.TracingPolicy;

/**
 * Triggers Vert.x event bus messages with the default {@link TracingPolicy#PROPAGATE} policy, both inside and
 * outside an active trace, so a test can assert the {@code EventBusInstrumenterVertxTracer} honors the policy.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/25417">#25417</a>
 */
@Path("/eventbus")
@Produces(MediaType.APPLICATION_JSON)
public class EventBusResource {

    @Inject
    EventBus eventBus;

    /**
     * Sends a {@code PROPAGATE} message from the active request trace. The producer/consumer spans must be
     * created and parented to the incoming server span.
     */
    @GET
    @Path("/active-trace")
    public TraceData withinActiveTrace() {
        eventBus.publish(PROPAGATE_ADDRESS, "hello", propagate());

        TraceData data = new TraceData();
        data.message = "within active trace";
        return data;
    }

    /**
     * Sends a {@code PROPAGATE} message with no active trace, so it must not be traced. An {@code ALWAYS}
     * message is sent alongside it as a sentinel that is always traced, giving the test a deterministic span
     * count to wait for instead of asserting on the absence of spans.
     */
    @GET
    @Path("/no-active-trace")
    public TraceData withoutActiveTrace() throws InterruptedException {
        // Publish from a plain thread that carries neither a Vert.x context nor an active OpenTelemetry span,
        // reproducing a message that originates outside of any trace (the #25417 scenario).
        Thread publisher = new Thread(() -> {
            eventBus.publish(PROPAGATE_ADDRESS, "hello", propagate());
            eventBus.publish(ALWAYS_ADDRESS, "hello", new DeliveryOptions().setTracingPolicy(TracingPolicy.ALWAYS));
        });
        publisher.start();
        publisher.join();

        TraceData data = new TraceData();
        data.message = "without active trace";
        return data;
    }

    private static DeliveryOptions propagate() {
        return new DeliveryOptions().setTracingPolicy(TracingPolicy.PROPAGATE);
    }
}
