package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.quartz.QuartzScheduler;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class NonconcurrentJobDefinitionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Jobs.class))
            .overrideConfigKey("quarkus.scheduler.start-mode", "forced");

    @Inject
    QuartzScheduler scheduler;

    @Test
    public void testExecution() throws InterruptedException {
        scheduler.newJob("foo")
                .setTask(se -> {
                    Jobs.NONCONCURRENT_COUNTER.incrementAndGet();
                    try {
                        if (!Jobs.CONCURRENT_LATCH.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("nonconcurrent() execution blocked too long...");
                        }
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                    if (Jobs.NONCONCURRENT_COUNTER.get() == 1) {
                        // concurrent() executed >= 5x and nonconcurrent() 1x
                        Jobs.NONCONCURRENT_LATCH.countDown();
                    }
                })
                .setInterval("1s")
                .setNonconcurrent()
                .schedule();

        assertTrue(Jobs.NONCONCURRENT_LATCH.await(10, TimeUnit.SECONDS),
                String.format("nonconcurrent() executed: %sx", Jobs.NONCONCURRENT_COUNTER.get()));
    }

    static class Jobs {

        static final CountDownLatch NONCONCURRENT_LATCH = new CountDownLatch(1);
        static final CountDownLatch CONCURRENT_LATCH = new CountDownLatch(5);

        static final AtomicInteger NONCONCURRENT_COUNTER = new AtomicInteger(0);

        @Scheduled(identity = "bar", every = "1s")
        void concurrent() throws InterruptedException {
            CONCURRENT_LATCH.countDown();
        }

    }

}
