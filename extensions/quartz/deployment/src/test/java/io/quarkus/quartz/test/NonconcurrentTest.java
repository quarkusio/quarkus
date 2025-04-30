package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.quartz.Nonconcurrent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class NonconcurrentTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Jobs.class));

    @Test
    public void testExecution() throws InterruptedException {
        assertTrue(Jobs.NONCONCURRENT_LATCH.await(10, TimeUnit.SECONDS),
                String.format("nonconcurrent() executed: %sx", Jobs.NONCONCURRENT_COUNTER.get()));
    }

    static class Jobs {

        static final CountDownLatch NONCONCURRENT_LATCH = new CountDownLatch(1);
        static final CountDownLatch CONCURRENT_LATCH = new CountDownLatch(5);

        static final AtomicInteger NONCONCURRENT_COUNTER = new AtomicInteger(0);

        @Nonconcurrent
        @Scheduled(identity = "foo", every = "1s")
        void nonconcurrent() throws InterruptedException {
            NONCONCURRENT_COUNTER.incrementAndGet();
            if (!CONCURRENT_LATCH.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("nonconcurrent() execution blocked too long...");
            }
            if (NONCONCURRENT_COUNTER.get() == 1) {
                // concurrent() executed >= 5x and nonconcurrent() 1x
                NONCONCURRENT_LATCH.countDown();
            }
        }

        @Scheduled(identity = "bar", every = "1s")
        void concurrent() throws InterruptedException {
            CONCURRENT_LATCH.countDown();
        }

    }
}
