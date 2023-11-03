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

public class OverdueCronExecutionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Jobs.class)
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
            Thread.sleep(1250);
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

        static final String CRON = "0/1 * * * * ?";

        static final CountDownLatch LATCH = new CountDownLatch(1);

        @Scheduled(identity = "overdueJob", cron = CRON, overdueGracePeriod = "0.1s")
        void overdueJob() {
            LATCH.countDown();
        }

        @Scheduled(identity = "tolerantJob", cron = CRON, overdueGracePeriod = "2H")
        void tolerantJob() {
        }

        @Scheduled(identity = "gracePeriodFromConfigJob", cron = CRON, overdueGracePeriod = "{job.gracePeriod}")
        void gracePeriodFromConfigJob() {
        }

        @Scheduled(identity = "defaultGracePeriodJob", cron = CRON)
        void defaultGracePeriodJob() {
        }
    }
}
