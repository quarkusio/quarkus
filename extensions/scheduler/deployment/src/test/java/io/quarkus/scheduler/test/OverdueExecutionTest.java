package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.quarkus.test.QuarkusUnitTest;

public class OverdueExecutionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("quarkus.scheduler.overdue-grace-period=2H\njob.gracePeriod=2H"),
                            "application.properties"));

    @Inject
    Scheduler scheduler;

    @Test
    public void testExecution() {
        try {
            Trigger overdueJob = scheduler.getScheduledJob("overdueJob");
            Trigger tolerantJob = scheduler.getScheduledJob("tolerantJob");
            Trigger gracePeriodFromConfigJob = scheduler.getScheduledJob("gracePeriodFromConfigJob");
            Trigger defaultGracePeriodJob = scheduler.getScheduledJob("defaultGracePeriodJob");
            assertTrue(Jobs.LATCH.await(5, TimeUnit.SECONDS));
            scheduler.pause();
            Thread.sleep(250);
            assertTrue(overdueJob.isOverdue());
            assertFalse(tolerantJob.isOverdue());
            assertFalse(gracePeriodFromConfigJob.isOverdue());
            assertFalse(defaultGracePeriodJob.isOverdue());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(1);

        @Scheduled(identity = "overdueJob", every = "0.1s", overdueGracePeriod = "0.1s")
        void overdueJob() throws InterruptedException {
            LATCH.countDown();
        }

        @Scheduled(identity = "tolerantJob", every = "0.1s", overdueGracePeriod = "2H")
        void tolerantJob() throws InterruptedException {
        }

        @Scheduled(identity = "gracePeriodFromConfigJob", every = "0.1s", overdueGracePeriod = "{job.gracePeriod}")
        void gracePeriodFromConfigJob() throws InterruptedException {
        }

        @Scheduled(identity = "defaultGracePeriodJob", every = "0.1s")
        void defaultGracePeriodJob() throws InterruptedException {
        }
    }
}
