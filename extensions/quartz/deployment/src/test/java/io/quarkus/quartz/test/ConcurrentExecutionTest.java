package io.quarkus.quartz.test;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class ConcurrentExecutionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Jobs.class));

    @Test
    public void testNonconcurrentExecution() throws InterruptedException {
        if (Jobs.LATCH.await(5, TimeUnit.SECONDS)) {
            assertEquals(1, Jobs.COUNTER.get());
        } else {
            fail("Scheduled methods not executed");
        }
    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(3);
        static final AtomicInteger COUNTER = new AtomicInteger();

        @Scheduled(every = "1s")
        void concurrent() {
            LATCH.countDown();
        }

        @Scheduled(every = "1s", concurrentExecution = SKIP)
        void nonconcurrent() throws InterruptedException {
            COUNTER.incrementAndGet();
            if (!LATCH.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("");
            }
        }
    }
}
