package io.quarkus.signals.runtime.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public class ConcurrencyLimiterTest {

    @Test
    public void testInvalidLimit() {
        assertThatThrownBy(() -> new ConcurrencyLimiter(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConcurrencyLimiter(-3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testRunsBelowLimit() {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(3);
        List<String> executed = new ArrayList<>();
        limiter.run(() -> executed.add("a"), t -> {
        });
        limiter.run(() -> executed.add("b"), t -> {
        });
        limiter.run(() -> executed.add("c"), t -> {
        });
        assertThat(executed).containsExactly("a", "b", "c");
    }

    @Test
    public void testQueuesAtLimit() {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(1);
        List<String> executed = new ArrayList<>();

        // First action runs immediately but does not complete
        limiter.run(() -> executed.add("a"), t -> {
        });
        assertThat(executed).containsExactly("a");

        // Second action is queued because limit=1 and "a" hasn't completed
        limiter.run(() -> executed.add("b"), t -> {
        });
        assertThat(executed).containsExactly("a");

        // Complete "a" — "b" should be drained from the queue
        limiter.complete();
        assertThat(executed).containsExactly("a", "b");
    }

    @Test
    public void testDrainsInOrderOnComplete() {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(2);
        List<String> executed = new ArrayList<>();

        // Fill both slots
        limiter.run(() -> executed.add("a"), t -> {
        });
        limiter.run(() -> executed.add("b"), t -> {
        });
        assertThat(executed).containsExactly("a", "b");

        // Queue two more
        limiter.run(() -> executed.add("c"), t -> {
        });
        limiter.run(() -> executed.add("d"), t -> {
        });
        assertThat(executed).containsExactly("a", "b");

        // Complete "a" — "c" should drain
        limiter.complete();
        assertThat(executed).containsExactly("a", "b", "c");

        // Complete "b" — "d" should drain
        limiter.complete();
        assertThat(executed).containsExactly("a", "b", "c", "d");
    }

    @Test
    public void testCompleteWithoutQueuedActions() {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(2);
        List<String> executed = new ArrayList<>();

        limiter.run(() -> executed.add("a"), t -> {
        });
        limiter.complete();
        // No queued actions — complete is a no-op beyond decrementing
        assertThat(executed).containsExactly("a");

        // New action should still run immediately
        limiter.run(() -> executed.add("b"), t -> {
        });
        assertThat(executed).containsExactly("a", "b");
    }

    @Test
    public void testLimitOfOne() {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(1);
        List<String> executed = new ArrayList<>();

        limiter.run(() -> executed.add("1"), t -> {
        });
        limiter.run(() -> executed.add("2"), t -> {
        });
        limiter.run(() -> executed.add("3"), t -> {
        });
        assertThat(executed).containsExactly("1");

        limiter.complete();
        assertThat(executed).containsExactly("1", "2");

        limiter.complete();
        assertThat(executed).containsExactly("1", "2", "3");
    }

    @Test
    public void testExceptionDoesNotLeakSlot() {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(1);
        List<String> executed = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        // Action throws — slot must be released, error handler called
        limiter.run(() -> {
            throw new RuntimeException("boom");
        }, error::set);

        assertThat(error.get()).isInstanceOf(RuntimeException.class).hasMessage("boom");

        // Slot is free — next action runs immediately
        limiter.run(() -> executed.add("after"), t -> {
        });
        assertThat(executed).containsExactly("after");
    }

    @Test
    public void testExceptionInQueuedActionDoesNotLeakSlot() {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(1);
        List<String> executed = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        // "a" takes the slot
        limiter.run(() -> executed.add("a"), t -> {
        });
        // Throwing action is queued
        limiter.run(() -> {
            throw new RuntimeException("boom");
        }, error::set);
        // "c" is queued behind the throwing action
        limiter.run(() -> executed.add("c"), t -> {
        });
        assertThat(executed).containsExactly("a");

        // Complete "a" — drains the throwing action (error handler called), then drains "c"
        limiter.complete();
        assertThat(error.get()).isInstanceOf(RuntimeException.class).hasMessage("boom");
        assertThat(executed).containsExactly("a", "c");
    }

    @Test
    public void testDoubleCompleteDoesNotGoNegative() {
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(1);
        List<String> executed = new ArrayList<>();

        limiter.run(() -> executed.add("a"), t -> {
        });
        limiter.complete();
        limiter.complete(); // Extra — should be harmless

        // Next action runs
        limiter.run(() -> executed.add("b"), t -> {
        });
        assertThat(executed).containsExactly("a", "b");

        // Limit still enforced: "b" hasn't completed, so "c" is queued
        limiter.run(() -> executed.add("c"), t -> {
        });
        assertThat(executed).containsExactly("a", "b");

        limiter.complete();
        assertThat(executed).containsExactly("a", "b", "c");
    }

    @Test
    public void testConcurrentAccessRespectLimit() throws InterruptedException {
        int limit = 3;
        int totalActions = 100;
        ConcurrencyLimiter limiter = new ConcurrencyLimiter(limit);

        AtomicInteger active = new AtomicInteger();
        AtomicInteger peakConcurrency = new AtomicInteger();
        CountDownLatch allDone = new CountDownLatch(totalActions);
        CopyOnWriteArrayList<Integer> peakSnapshots = new CopyOnWriteArrayList<>();

        ExecutorService submitter = Executors.newFixedThreadPool(8);
        ExecutorService completer = Executors.newFixedThreadPool(4);

        for (int i = 0; i < totalActions; i++) {
            submitter.submit(() -> {
                limiter.run(() -> {
                    int current = active.incrementAndGet();
                    peakConcurrency.updateAndGet(peak -> Math.max(peak, current));
                    peakSnapshots.add(current);
                    // Complete asynchronously to avoid recursive tryDrain
                    completer.submit(() -> {
                        active.decrementAndGet();
                        limiter.complete();
                        allDone.countDown();
                    });
                }, t -> allDone.countDown());
            });
        }

        assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();
        submitter.shutdown();
        completer.shutdown();

        assertThat(peakConcurrency.get()).isLessThanOrEqualTo(limit);
        assertThat(peakSnapshots).hasSize(totalActions);
        assertThat(peakSnapshots).allSatisfy(v -> assertThat(v).isLessThanOrEqualTo(limit));
    }
}
