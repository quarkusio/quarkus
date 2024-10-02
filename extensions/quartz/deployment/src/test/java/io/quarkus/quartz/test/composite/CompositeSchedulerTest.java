package io.quarkus.quartz.test.composite;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.quartz.QuartzScheduler;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.runtime.Constituent;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Identifier;

public class CompositeSchedulerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Jobs.class))
            .overrideConfigKey("quarkus.scheduler.use-composite-scheduler", "true");

    @Constituent
    QuartzScheduler quartz;

    @Constituent
    @Identifier("SIMPLE")
    Scheduler simple;

    @Inject
    Scheduler composite;

    @Test
    public void testExecution() throws InterruptedException {
        assertTrue(Jobs.simpleLatch.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.quartzLatch.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.autoLatch.await(5, TimeUnit.SECONDS));

        assertNull(quartz.getScheduledJob("simple"));
        assertNotNull(quartz.getScheduledJob("quartz"));
        assertNotNull(quartz.getScheduledJob("auto"));

        assertNotNull(simple.getScheduledJob("simple"));
        assertNull(simple.getScheduledJob("quartz"));
        assertNull(simple.getScheduledJob("auto"));

        assertNotNull(composite.getScheduledJob("quartz"));
        assertNotNull(composite.getScheduledJob("auto"));
        assertNotNull(composite.getScheduledJob("simple"));

        composite.pause();
        Jobs.reset();
        assertFalse(composite.isRunning());
        assertFalse(Jobs.simpleLatch.await(2, TimeUnit.SECONDS));

        composite.resume();
        assertTrue(composite.isRunning());
        assertTrue(Jobs.simpleLatch.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.quartzLatch.await(5, TimeUnit.SECONDS));
        assertTrue(Jobs.autoLatch.await(5, TimeUnit.SECONDS));
    }

    static class Jobs {

        static CountDownLatch simpleLatch = new CountDownLatch(1);
        static CountDownLatch quartzLatch = new CountDownLatch(1);
        static CountDownLatch autoLatch = new CountDownLatch(1);

        static void reset() {
            simpleLatch = new CountDownLatch(1);
            quartzLatch = new CountDownLatch(1);
            autoLatch = new CountDownLatch(1);
        }

        @Scheduled(identity = "simple", every = "1s", executeWith = Scheduled.SIMPLE)
        void simple() {
            simpleLatch.countDown();
        }

        @Scheduled(identity = "quartz", every = "1s", executeWith = Scheduled.QUARTZ)
        void quartz() {
            quartzLatch.countDown();
        }

        @Scheduled(identity = "auto", every = "1s")
        void auto() {
            autoLatch.countDown();
        }

    }
}
