package io.quarkus.signals.deployment.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receivers;
import io.quarkus.signals.Receivers.ExecutionModel;
import io.quarkus.signals.Receivers.Registration;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;

public class BlockingConcurrencyLimitTest extends AbstractSignalTest {

    static final int CONCURRENCY_LIMIT = 2;
    static final int TOTAL_RECEIVERS = 5;

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Cmd.class))
            .overrideRuntimeConfigKey("quarkus.signals.receivers.blocking-concurrency-limit",
                    String.valueOf(CONCURRENCY_LIMIT));

    @Inject
    Signal<Cmd> signal;

    @Inject
    Receivers receivers;

    @Test
    public void testConcurrencyLimit() throws InterruptedException {
        AtomicInteger activeTasks = new AtomicInteger(0);
        AtomicInteger peakConcurrency = new AtomicInteger(0);
        CountDownLatch batchStarted = new CountDownLatch(CONCURRENCY_LIMIT);
        CountDownLatch gate = new CountDownLatch(1);
        CountDownLatch allDone = new CountDownLatch(TOTAL_RECEIVERS);

        List<Registration> registrations = new ArrayList<>(TOTAL_RECEIVERS);
        for (int i = 0; i < TOTAL_RECEIVERS; i++) {
            registrations.add(receivers.newReceiver(Cmd.class)
                    .setExecutionModel(ExecutionModel.BLOCKING)
                    .notify(ctx -> {
                        int current = activeTasks.incrementAndGet();
                        peakConcurrency.updateAndGet(peak -> Math.max(peak, current));
                        batchStarted.countDown();
                        try {
                            gate.await(defaultTimeout().toMillis(), TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            activeTasks.decrementAndGet();
                            allDone.countDown();
                        }
                    }));
        }

        try {
            signal.publish(new Cmd());

            // Wait for the first batch of receivers to start
            assertTrue(batchStarted.await(defaultTimeout().toMillis(), TimeUnit.MILLISECONDS),
                    "First batch of receivers should start within timeout");

            assertTrue(peakConcurrency.get() <= CONCURRENCY_LIMIT,
                    "Peak concurrency " + peakConcurrency.get() + " exceeded limit " + CONCURRENCY_LIMIT);

            // Open the gate so all receivers can finish
            gate.countDown();

            assertTrue(allDone.await(defaultTimeout().toMillis(), TimeUnit.MILLISECONDS),
                    "All receivers should complete within timeout");
        } finally {
            for (Receivers.Registration reg : registrations) {
                reg.unregister();
            }
        }
    }

    record Cmd() {
    }
}
