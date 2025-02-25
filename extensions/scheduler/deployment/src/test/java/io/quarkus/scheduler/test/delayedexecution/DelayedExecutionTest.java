package io.quarkus.scheduler.test.delayedexecution;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.DelayedExecution;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class DelayedExecutionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Jobs.class));

    @Test
    public void testSimpleScheduledJobs() throws InterruptedException {
        assertTrue(Jobs.EVENT_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.LATCH1.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.LATCH2.await(5, TimeUnit.SECONDS));
    }

    static class Jobs {

        static final CountDownLatch LATCH1 = new CountDownLatch(1);
        static final CountDownLatch LATCH2 = new CountDownLatch(1);
        static final CountDownLatch EVENT_LATCH = new CountDownLatch(2);

        @Scheduled(identity = "foo", every = "1s", executionMaxDelay = "500ms")
        static void everySecond() {
            LATCH1.countDown();
        }

        void start(@Observes StartupEvent event, Scheduler scheduler) {
            scheduler.newJob("bar")
                    .setInterval("1s")
                    .setExecutionMaxDelay("500ms")
                    .setTask(se -> {
                        LATCH2.countDown();
                    }).schedule();
        }

        void onDelay(@Observes DelayedExecution delayedExecution) {
            assertTrue(delayedExecution.getDelay() < 500);
            String id = delayedExecution.getExecution().getTrigger().getId();
            if ("foo".equals(id) || "bar".equals(id)) {
                EVENT_LATCH.countDown();
            } else {
                throw new IllegalStateException("Invalid job identity: " + id);
            }
        }

    }

}
