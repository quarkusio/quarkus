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

// see https://github.com/quarkusio/quarkus/issues/52784
public class ZeroDelaySchedulerExecutionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Jobs.class));

    @Test
    public void testJobsWithZeroDelay() throws InterruptedException {
        assertTrue(Jobs.LATCH1.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.LATCH2.await(5, TimeUnit.SECONDS));
    }

    static class Jobs {

        static final CountDownLatch LATCH1 = new CountDownLatch(1);
        static final CountDownLatch LATCH2 = new CountDownLatch(1);

        @Scheduled(identity = "zeroDelay", every = "1s", executionMaxDelay = "0ms")
        static void everySecondZeroDelay() {
            LATCH1.countDown();
        }

        void start(@Observes StartupEvent event, Scheduler scheduler) {
            scheduler.newJob("zeroDelayProgrammatic")
                    .setInterval("1s")
                    .setExecutionMaxDelay("0ms")
                    .setTask(se -> {
                        LATCH2.countDown();
                    }).schedule();
        }

        void onDelay(@Observes DelayedExecution delayedExecution) {
            throw new IllegalStateException("DelayedExecution event should not be fired if delay = 0");
        }

    }
}
