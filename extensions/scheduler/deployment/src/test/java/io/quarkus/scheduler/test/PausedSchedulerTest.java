package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.SchedulerPaused;
import io.quarkus.scheduler.SchedulerResumed;
import io.quarkus.scheduler.Trigger;
import io.quarkus.test.QuarkusExtensionTest;

public class PausedSchedulerTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PausedSchedulerTest.Jobs.class));

    @Inject
    Scheduler scheduler;

    @Test
    public void testSchedulerPauseResume() throws InterruptedException {
        assertTrue(scheduler.isRunning());
        Trigger trigger = scheduler.getScheduledJob(Jobs.IDENTITY);
        assertNotNull(trigger);

        scheduler.pause();
        // Note that pause() only prevents triggers from firing. An execution triggered before the pause may still be
        // in flight and complete afterwards; it carries a fire time from before the pause and is therefore ignored.
        Instant pausedAt = Instant.now();
        Jobs.WATERMARK.set(pausedAt);
        assertFalse(scheduler.isRunning());
        assertTrue(Jobs.PAUSED_EVENT.get());

        // No job may be executed while the scheduler is paused; this returns early if one is
        assertFalse(Jobs.FIRES.tryAcquire(2, TimeUnit.SECONDS), "Job executed while the scheduler was paused");
        // And no trigger may fire at all, even if no execution followed within the window above
        assertFalse(firedAfter(trigger, pausedAt),
                () -> "Trigger fired while the scheduler was paused: " + trigger.getPreviousFireTime());

        // Only an execution triggered from now on may release a permit
        Jobs.WATERMARK.set(Instant.now());
        scheduler.resume();
        assertTrue(scheduler.isRunning());
        assertTrue(Jobs.RESUMED_EVENT.get());

        assertTrue(Jobs.FIRES.tryAcquire(4, TimeUnit.SECONDS), "Job not executed after the scheduler was resumed");
        assertTrue(Jobs.EVENT_LATCH.await(4, TimeUnit.SECONDS));
    }

    static boolean firedAfter(Trigger trigger, Instant instant) {
        Instant previousFireTime = trigger.getPreviousFireTime();
        return previousFireTime != null && previousFireTime.isAfter(instant);
    }

    @Singleton
    static class Jobs {

        static final String IDENTITY = "every-second";

        static final Semaphore FIRES = new Semaphore(0);
        static final CountDownLatch EVENT_LATCH = new CountDownLatch(2);
        static final AtomicBoolean PAUSED_EVENT = new AtomicBoolean();
        static final AtomicBoolean RESUMED_EVENT = new AtomicBoolean();
        static final AtomicReference<Instant> WATERMARK = new AtomicReference<>(Instant.MAX);

        @Scheduled(identity = IDENTITY, every = "1s")
        void everySecond(ScheduledExecution execution) {
            // The fire time is the moment the trigger fired, i.e. it is unaffected by the delay between the trigger
            // firing and this method being invoked
            if (execution.getFireTime().isAfter(WATERMARK.get())) {
                FIRES.release();
            }
        }

        void onPause(@Observes SchedulerPaused e) {
            PAUSED_EVENT.set(true);
        }

        void onPauseAsync(@ObservesAsync SchedulerPaused e) {
            EVENT_LATCH.countDown();
        }

        void onResume(@Observes SchedulerResumed e) {
            RESUMED_EVENT.set(true);
        }

        void onResumeAsync(@ObservesAsync SchedulerResumed e) {
            EVENT_LATCH.countDown();
        }
    }

}
