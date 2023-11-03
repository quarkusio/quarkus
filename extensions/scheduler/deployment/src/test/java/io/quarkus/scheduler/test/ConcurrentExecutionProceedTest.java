package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class ConcurrentExecutionProceedTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class));

    @Test
    public void testExecution() {
        try {
            // Wait until Jobs#concurrent() is executed 3x and skipped 0x
            if (Jobs.START_LATCH.await(10, TimeUnit.SECONDS)) {
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

        static final CountDownLatch START_LATCH = new CountDownLatch(3);

        @Scheduled(every = "1s")
        void concurrent() throws InterruptedException {
            START_LATCH.countDown();
            if (!BLOCKING_LATCH.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("concurrent() execution blocked too long...");
            }
        }
    }
}
