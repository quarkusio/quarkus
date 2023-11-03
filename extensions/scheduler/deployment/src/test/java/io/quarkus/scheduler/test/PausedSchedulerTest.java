package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.SchedulerPaused;
import io.quarkus.scheduler.SchedulerResumed;
import io.quarkus.test.QuarkusUnitTest;

public class PausedSchedulerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PausedSchedulerTest.Jobs.class));

    @Inject
    Scheduler scheduler;

    @Inject
    Jobs jobs;

    @Test
    public void testSchedulerPauseResume() throws InterruptedException {
        assertTrue(scheduler.isRunning());

        scheduler.pause();
        assertFalse(scheduler.isRunning());
        assertTrue(Jobs.PAUSED_EVENT.get());

        // this should have no effect because the scheduler is paused
        jobs.running.set(true);
        assertFalse(Jobs.JOB_LATCH.await(4, TimeUnit.SECONDS));

        scheduler.resume();
        assertTrue(scheduler.isRunning());
        assertTrue(Jobs.RESUMED_EVENT.get());

        assertTrue(Jobs.JOB_LATCH.await(4, TimeUnit.SECONDS));
        assertTrue(Jobs.EVENT_LATCH.await(4, TimeUnit.SECONDS));
    }

    @Singleton
    static class Jobs {

        static final CountDownLatch JOB_LATCH = new CountDownLatch(1);
        static final CountDownLatch EVENT_LATCH = new CountDownLatch(2);
        static final AtomicBoolean PAUSED_EVENT = new AtomicBoolean();
        static final AtomicBoolean RESUMED_EVENT = new AtomicBoolean();

        private final AtomicBoolean running = new AtomicBoolean(false);

        @Scheduled(every = "1s")
        void everySecond() {
            if (running.get()) {
                JOB_LATCH.countDown();
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
