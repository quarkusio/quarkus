package io.quarkus.devui.observability.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;

class TelemetryStoreTest {

    @Test
    void recordStoresInSnapshot() {
        TelemetryStore<String> store = new TelemetryStore<>(new CountBoundingStrategy(2));
        store.record("a");
        store.record("b");
        store.record("c"); // evicts "a"
        assertThat(store.snapshot()).containsExactly("b", "c");
        assertThat(store.size()).isEqualTo(2);
    }

    @Test
    void streamDeliversNewRecordsToSubscriber() {
        TelemetryStore<String> store = new TelemetryStore<>(new CountBoundingStrategy(10));
        AssertSubscriber<String> subscriber = store.stream()
                .subscribe().withSubscriber(AssertSubscriber.create(10));
        store.record("x");
        store.record("y");
        subscriber.awaitItems(2, Duration.ofSeconds(5)).assertItems("x", "y");
    }

    @Test
    void recordNeverBlocksWithNoSubscriber() {
        TelemetryStore<String> store = new TelemetryStore<>(new CountBoundingStrategy(10));
        // No subscriber: broadcaster must drop silently, record must still store.
        store.record("only-buffered");
        assertThat(store.snapshot()).containsExactly("only-buffered");
    }

    @Test
    void slowSubscriberDoesNotBlockRecordingOrLoseBufferedData() {
        TelemetryStore<Integer> store = new TelemetryStore<>(new CountBoundingStrategy(1000));
        // Subscriber requests only 1 item, then stops requesting -> overflow.
        AssertSubscriber<Integer> subscriber = store.stream()
                .subscribe().withSubscriber(AssertSubscriber.create(1));
        for (int i = 0; i < 500; i++) {
            store.record(i); // must not throw / must not block
        }
        // Buffer (source of truth) has everything up to the bound; stream dropped overflow.
        assertThat(store.size()).isEqualTo(500);
        subscriber.assertNotTerminated(); // dropped, not failed
    }

    @Test
    void concurrentRecordingIsSafe() throws InterruptedException {
        int threads = 4;
        int perThread = 250;
        int total = threads * perThread; // 1000
        TelemetryStore<Integer> store = new TelemetryStore<>(new CountBoundingStrategy(total));
        AssertSubscriber<Integer> subscriber = store.stream()
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        CountDownLatch start = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < perThread; i++) {
                    store.record(i);
                }
            });
            workers.add(worker);
            worker.start();
        }
        start.countDown();
        for (Thread worker : workers) {
            worker.join();
        }

        subscriber.assertNotTerminated(); // no onError from concurrent emissions
        assertThat(store.size()).isEqualTo(total);
        subscriber.awaitItems(total, Duration.ofSeconds(5));
    }

    @Test
    void clearEmptiesBuffer() {
        TelemetryStore<String> store = new TelemetryStore<>(new CountBoundingStrategy(10));
        store.record("a");
        store.clear();
        assertThat(store.snapshot()).isEmpty();
    }
}
