package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receivers;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

/**
 * Verifies that the {@link Uni} returned by the reactive emission methods
 * ({@link Signal.ReactiveEmission#publish(Object)}, {@link Signal.ReactiveEmission#send(Object)},
 * {@link Signal.ReactiveEmission#request(Object, Class)})
 * is lazy: no signal is emitted until the {@link Uni} is subscribed, and each subscription
 * triggers a new, independent signal emission.
 */
public class ResubscriptionTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Ping.class));

    @Inject
    Signal<Ping> signal;

    @Inject
    Receivers receivers;

    @Test
    public void testPublishEmitsOnEachSubscription() {
        List<String> received = new CopyOnWriteArrayList<>();

        var reg = receivers.newReceiver(Ping.class)
                .notify(ctx -> {
                    received.add(ctx.signal().id());
                });
        try {
            Uni<Void> uni = signal.reactive().publish(new Ping("p"));

            // First subscription
            uni.ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
            assertEquals(1, received.size());

            // Second subscription of the same Uni — should trigger a new emission
            uni.ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
            assertEquals(2, received.size());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testSendEmitsOnEachSubscription() {
        AtomicInteger count = new AtomicInteger();

        var reg = receivers.newReceiver(Ping.class)
                .notify(ctx -> {
                    count.incrementAndGet();
                });
        try {
            Uni<Void> uni = signal.reactive().send(new Ping("s"));

            // First subscription
            uni.ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
            assertEquals(1, count.get());

            // Second subscription of the same Uni — should trigger a new emission
            uni.ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
            assertEquals(2, count.get());
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testRequestEmitsOnEachSubscription() {
        AtomicInteger count = new AtomicInteger();

        var reg = receivers.newReceiver(Ping.class)
                .setResponseType(String.class)
                .notify(ctx -> {
                    return Uni.createFrom().item("reply_" + count.incrementAndGet());
                });
        try {
            Uni<String> uni = signal.reactive().request(new Ping("r"), String.class);

            // First subscription
            String first = uni.ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
            assertEquals("reply_1", first);

            // Second subscription of the same Uni — should trigger a new emission
            String second = uni.ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
            assertEquals("reply_2", second);
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testPublishNoEmissionWithoutSubscription() {
        AtomicInteger count = new AtomicInteger();

        var reg = receivers.newReceiver(Ping.class)
                .notify(ctx -> {
                    count.incrementAndGet();
                });
        try {
            // Create the Uni but do NOT subscribe
            signal.reactive().publish(new Ping("noop"));

            Awaitility.await()
                    .during(Duration.ofMillis(500))
                    .atMost(Duration.ofSeconds(1))
                    .until(() -> count.get() == 0);
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testSendNoEmissionWithoutSubscription() {
        AtomicInteger count = new AtomicInteger();

        var reg = receivers.newReceiver(Ping.class)
                .notify(ctx -> {
                    count.incrementAndGet();
                });
        try {
            // Create the Uni but do NOT subscribe
            signal.reactive().send(new Ping("noop"));

            Awaitility.await()
                    .during(Duration.ofMillis(500))
                    .atMost(Duration.ofSeconds(1))
                    .until(() -> count.get() == 0);
        } finally {
            reg.unregister();
        }
    }

    @Test
    public void testRequestNoEmissionWithoutSubscription() {
        AtomicInteger count = new AtomicInteger();

        var reg = receivers.newReceiver(Ping.class)
                .setResponseType(String.class)
                .notify(ctx -> {
                    count.incrementAndGet();
                    return Uni.createFrom().item("reply");
                });
        try {
            // Create the Uni but do NOT subscribe
            signal.reactive().request(new Ping("noop"), String.class);

            Awaitility.await()
                    .during(Duration.ofMillis(500))
                    .atMost(Duration.ofSeconds(1))
                    .until(() -> count.get() == 0);
        } finally {
            reg.unregister();
        }
    }

    record Ping(String id) {
    }
}
