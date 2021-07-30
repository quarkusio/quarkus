package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class PausedMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PausedMethodTest.Jobs.class));

    private static final String IDENTITY = "myScheduled";

    @Inject
    Scheduler scheduler;

    @Test
    public void testPause() throws InterruptedException {
        assertFalse(Jobs.LATCH.await(3, TimeUnit.SECONDS));
        assertTrue(scheduler.isPaused(IDENTITY));
    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(1);

        @Scheduled(every = "1s", identity = IDENTITY)
        void countDownSecond() {
            LATCH.countDown();
        }

        void pause(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE - 1) StartupEvent event, Scheduler scheduler) {
            // Pause the job before the scheduler starts
            scheduler.pause(IDENTITY);
        }
    }
}
