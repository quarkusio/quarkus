package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that {@link Signal#send(Object)} rotates across matching receivers in round-robin order.
 */
public class SignalRoundRobinTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Receivers.class, Ping.class));

    @Inject
    Signal<Ping> ping;

    @Inject
    Receivers receivers;

    @Test
    public void testRoundRobin() {
        receivers.sequence.clear();

        // Send 4 signals — should cycle across the two unqualified receivers
        for (int i = 0; i < 4; i++) {
            ping.send(new Ping(i));
        }
        Awaitility.await().until(() -> receivers.sequence.size() >= 4);
        assertEquals(4, receivers.sequence.size());

        // Each receiver should have been invoked exactly twice
        long firstCount = receivers.sequence.stream().filter(s -> s.startsWith("first_")).count();
        long secondCount = receivers.sequence.stream().filter(s -> s.startsWith("second_")).count();
        assertEquals(2, firstCount, "first receiver should be called twice");
        assertEquals(2, secondCount, "second receiver should be called twice");
    }

    @Singleton
    public static class Receivers {

        final List<String> sequence = new CopyOnWriteArrayList<>();

        void first(@Receives Ping ping) {
            sequence.add("first_" + ping.id());
        }

        void second(@Receives Ping ping) {
            sequence.add("second_" + ping.id());
        }
    }

    record Ping(int id) {
    }
}
