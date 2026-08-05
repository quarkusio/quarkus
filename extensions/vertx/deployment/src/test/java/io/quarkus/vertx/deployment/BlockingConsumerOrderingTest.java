package io.quarkus.vertx.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;

public class BlockingConsumerOrderingTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(OrderingConsumers.class));

    @Inject
    EventBus eventBus;

    @Inject
    OrderingConsumers consumers;

    @Test
    public void testOrderedBlockingConsumerProcessesSequentially() throws InterruptedException {
        consumers.reset();
        int messageCount = 5;
        OrderingConsumers.orderedLatch = new CountDownLatch(messageCount);

        for (int i = 0; i < messageCount; i++) {
            eventBus.send("ordered-blocking", i);
        }

        assertThat(OrderingConsumers.orderedLatch.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(OrderingConsumers.orderedResults).hasSize(messageCount);
        for (int i = 0; i < messageCount; i++) {
            assertThat(OrderingConsumers.orderedResults.get(i)).isEqualTo(i);
        }
        assertThat(OrderingConsumers.orderedMaxConcurrency).isEqualTo(1);
    }

    @Test
    public void testUnorderedBlockingConsumerAllowsConcurrency() throws InterruptedException {
        consumers.reset();
        int messageCount = 5;
        OrderingConsumers.unorderedLatch = new CountDownLatch(messageCount);

        for (int i = 0; i < messageCount; i++) {
            eventBus.send("unordered-blocking", i);
        }

        assertThat(OrderingConsumers.unorderedLatch.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(OrderingConsumers.unorderedResults).hasSize(messageCount);
    }

    @ApplicationScoped
    static class OrderingConsumers {

        static volatile CountDownLatch orderedLatch;
        static volatile CountDownLatch unorderedLatch;
        static final List<Integer> orderedResults = new CopyOnWriteArrayList<>();
        static final List<Integer> unorderedResults = new CopyOnWriteArrayList<>();
        static volatile int orderedMaxConcurrency = 0;
        static volatile int orderedCurrentConcurrency = 0;
        static final Object concurrencyLock = new Object();

        void reset() {
            orderedResults.clear();
            unorderedResults.clear();
            orderedMaxConcurrency = 0;
            orderedCurrentConcurrency = 0;
        }

        @ConsumeEvent(value = "ordered-blocking", blocking = true, ordered = true)
        void consumeOrdered(int value) {
            synchronized (concurrencyLock) {
                orderedCurrentConcurrency++;
                orderedMaxConcurrency = Math.max(orderedMaxConcurrency, orderedCurrentConcurrency);
            }
            try {
                // Small sleep to give concurrent messages a chance to interleave
                Thread.sleep(50);
                orderedResults.add(value);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                synchronized (concurrencyLock) {
                    orderedCurrentConcurrency--;
                }
                orderedLatch.countDown();
            }
        }

        @ConsumeEvent(value = "unordered-blocking", blocking = true)
        void consumeUnordered(int value) {
            try {
                Thread.sleep(50);
                unorderedResults.add(value);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                unorderedLatch.countDown();
            }
        }
    }
}
