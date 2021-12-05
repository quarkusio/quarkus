package io.quarkus.scheduler.test;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.SkippedExecution;
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
                // Unblock all executions
                Jobs.BLOCKING_LATCH.countDown();
            } else {
                fail("Jobs were not executed in 10 seconds!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    static class Jobs {

        static final CountDownLatch BLOCKING_LATCH = new CountDownLatch(1);

        static final AtomicInteger COUNTER = new AtomicInteger(0);
        static final CountDownLatch SKIPPED_LATCH = new CountDownLatch(1);

        @Scheduled(every = "1s", concurrentExecution = SKIP)
        void nonconcurrent() throws InterruptedException {
            COUNTER.incrementAndGet();
            if (!BLOCKING_LATCH.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("nonconcurrent() execution blocked too long...");
            }
        }

        void onSkip(@Observes SkippedExecution event) {
            SKIPPED_LATCH.countDown();
        }
    }
}
