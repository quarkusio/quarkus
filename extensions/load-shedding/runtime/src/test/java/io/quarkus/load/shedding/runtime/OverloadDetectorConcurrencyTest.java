package io.quarkus.load.shedding.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

public class OverloadDetectorConcurrencyTest {
    private static final int INITIAL_LIMIT = 4;

    private static final int MAX_LIMIT = 8;

    private static final int THREADS = 8;

    private static final int ITERATIONS = 500_000;

    @Test
    public void everyRequestThatBeginsAlsoEnds() throws InterruptedException {
        OverloadDetector detector = new OverloadDetector(new TestConfig.Builder()
                .withInitialLimit(INITIAL_LIMIT)
                .withMaxLimit(MAX_LIMIT)
                .build());

        runWorkload(detector);

        assertThat(detector.currentRequests()).isZero();
        assertThat(detector.isOverloaded()).isFalse();
        assertThat(detector.overloadDurationNanos()).isZero();
        assertThat(detector.currentLimit()).isBetween(1L, (long) MAX_LIMIT);
    }

    private void runWorkload(OverloadDetector detector) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    // interruption is meaningless in this test
                }
                for (int j = 0; j < ITERATIONS; j++) {
                    detector.requestBegin();
                    detector.requestEnd(1_000);
                }
            });
            thread.start();
            threads.add(thread);
        }

        start.countDown();

        for (Thread thread : threads) {
            thread.join(Duration.ofSeconds(10));
            assertThat(thread.isAlive()).isFalse();
        }
    }
}
