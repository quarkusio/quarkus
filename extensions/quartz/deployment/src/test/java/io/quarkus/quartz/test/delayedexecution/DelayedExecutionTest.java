package io.quarkus.quartz.test.delayedexecution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.DelayedExecution;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class DelayedExecutionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Jobs.class));

    @Test
    public void testSimpleScheduledJobs() throws InterruptedException {
        assertTrue(Jobs.EVENT_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.LATCH.await(5, TimeUnit.SECONDS));
    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(1);
        static final CountDownLatch EVENT_LATCH = new CountDownLatch(1);

        @Scheduled(identity = "foo", every = "1s", executionMaxDelay = "500ms")
        static void everySecond() {
            LATCH.countDown();
        }

        void onDelay(@Observes DelayedExecution delayedExecution) {
            assertTrue(delayedExecution.getDelay() < 500);
            assertEquals("foo", delayedExecution.getExecution().getTrigger().getId());
            EVENT_LATCH.countDown();
        }

    }

}
