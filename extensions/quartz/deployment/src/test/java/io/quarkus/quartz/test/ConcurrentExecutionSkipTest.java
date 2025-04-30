package io.quarkus.quartz.test;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.FailedExecution;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.SkippedExecution;
import io.quarkus.scheduler.SuccessfulExecution;
import io.quarkus.test.QuarkusUnitTest;

public class ConcurrentExecutionSkipTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class));

    @Test
    public void testExecution() {
        try {
            // Wait until Jobs#nonconcurrent() is executed 1x and skipped 1x
            if (Jobs.SKIPPED_LATCH.await(10, TimeUnit.SECONDS)) {
                // Exactly one job is blocked
                assertEquals(1, Jobs.COUNTER.get());
                // Skipped Execution does not fire SuccessfulExecution event
                assertEquals(0, Jobs.SUCCESS_COUNTER.get());
                // Unblock all executions
                Jobs.BLOCKING_LATCH.countDown();
            } else {
                fail("Jobs were not executed in 10 seconds!");
            }

            assertTrue(Jobs.FAILED_LATCH.await(5, TimeUnit.SECONDS));
            assertTrue(Jobs.FAILURE_COUNTER.get() > 0);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    static class Jobs {

        static final CountDownLatch BLOCKING_LATCH = new CountDownLatch(1);

        static final AtomicInteger COUNTER = new AtomicInteger(0);
        static final AtomicInteger FAILING_COUNTER = new AtomicInteger(0);
        static final AtomicInteger SUCCESS_COUNTER = new AtomicInteger(0);
        static final AtomicInteger FAILURE_COUNTER = new AtomicInteger(0);
        static final CountDownLatch SKIPPED_LATCH = new CountDownLatch(1);
        static final CountDownLatch FAILED_LATCH = new CountDownLatch(1);

        @Scheduled(every = "1s", concurrentExecution = SKIP)
        void nonconcurrent() throws InterruptedException {
            COUNTER.incrementAndGet();
            if (!BLOCKING_LATCH.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("nonconcurrent() execution blocked too long...");
            }
        }

        @Scheduled(every = "1s", concurrentExecution = SKIP)
        void failing() {
            if (FAILING_COUNTER.incrementAndGet() > 2) {
                FAILED_LATCH.countDown();
            }
            throw new IllegalStateException();
        }

        void onSkip(@Observes SkippedExecution event) {
            SKIPPED_LATCH.countDown();
        }

        void onSuccess(@Observes SuccessfulExecution event) {
            SUCCESS_COUNTER.incrementAndGet();
        }

        void onFailure(@Observes FailedExecution event) {
            FAILURE_COUNTER.incrementAndGet();
        }
    }
}
