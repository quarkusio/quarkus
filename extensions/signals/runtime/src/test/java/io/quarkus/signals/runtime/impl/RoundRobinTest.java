package io.quarkus.signals.runtime.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

public class RoundRobinTest {

    @Test
    public void testEmpty() {
        RoundRobin<String> rr = new RoundRobin<>(List.of());
        assertThat(rr.next()).isNull();
        assertThat(rr.isEmpty()).isTrue();
        assertThat(rr.size()).isZero();
        assertThat(rr.elements()).isEmpty();
    }

    @Test
    public void testSingleElement() {
        RoundRobin<String> rr = new RoundRobin<>(List.of("a"));
        assertThat(rr.size()).isEqualTo(1);
        assertThat(rr.isEmpty()).isFalse();
        for (int i = 0; i < 5; i++) {
            assertThat(rr.next()).isEqualTo("a");
        }
    }

    @Test
    public void testCyclesThroughElements() {
        RoundRobin<String> rr = new RoundRobin<>(List.of("a", "b", "c"));
        assertThat(rr.size()).isEqualTo(3);
        List<String> results = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            results.add(rr.next());
        }
        assertThat(results).containsExactly("a", "b", "c", "a", "b", "c", "a", "b", "c");
    }

    @Test
    public void testElementsAreImmutable() {
        List<String> original = new ArrayList<>(List.of("a", "b"));
        RoundRobin<String> rr = new RoundRobin<>(original);
        original.add("c");
        assertThat(rr.size()).isEqualTo(2);
    }

    @Test
    public void testIteratorMatchesElements() {
        RoundRobin<String> rr = new RoundRobin<>(List.of("x", "y"));
        List<String> iterated = new ArrayList<>();
        rr.iterator().forEachRemaining(iterated::add);
        assertThat(iterated).containsExactly("x", "y");
    }

    @Test
    public void testNoOverflowAfterMaxInt() {
        List<String> items = List.of("a", "b");
        RoundRobin<String> rr = new RoundRobin<>(items, Integer.MAX_VALUE - 2);
        // After overflow the counter is negative, but next() must still return valid elements
        for (int i = 0; i < 10; i++) {
            assertThat(rr.next()).isIn("a", "b");
        }
    }

    @Test
    public void testConcurrentAccessDistributesEvenly() throws InterruptedException {
        List<String> items = List.of("a", "b", "c");
        RoundRobin<String> rr = new RoundRobin<>(items);
        int threads = 6;
        int callsPerThread = 300;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
        CountDownLatch latch = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < callsPerThread; i++) {
                        counts.merge(rr.next(), 1, Integer::sum);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();
        int totalCalls = threads * callsPerThread;
        assertThat(counts.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(totalCalls);
        assertThat(counts.keySet()).isEqualTo(Set.of("a", "b", "c"));
        for (int count : counts.values()) {
            assertThat(count).isEqualTo(totalCalls / items.size());
        }
    }
}
