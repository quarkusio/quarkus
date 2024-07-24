package io.quarkus.quartz.test.composite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

public class CompositeJobDefinitionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
            })
            .overrideConfigKey("quarkus.scheduler.use-composite-scheduler", "true")
            .overrideConfigKey("quarkus.scheduler.start-mode", "forced");

    @Constituent
    QuartzScheduler quartz;

    @Constituent
    @Identifier("SIMPLE")
    Scheduler simple;

    @Inject
    Scheduler composite;

    static CountDownLatch simpleLatch = new CountDownLatch(1);
    static CountDownLatch quartzLatch = new CountDownLatch(1);
    static CountDownLatch autoLatch = new CountDownLatch(1);

    static void reset() {
        simpleLatch = new CountDownLatch(1);
        quartzLatch = new CountDownLatch(1);
        autoLatch = new CountDownLatch(1);
    }

    @Test
    public void testExecution() throws InterruptedException {

        assertEquals("Scheduler implementation not available: bar",
                assertThrows(IllegalArgumentException.class, () -> composite.newJob("foo").setExecuteWith("bar")).getMessage());

        composite.newJob("simple")
                .setInterval("1s")
                .setExecuteWith(Scheduled.SIMPLE)
                .setTask(se -> {
                    simpleLatch.countDown();
                }).schedule();

        composite.newJob("quartz")
                .setInterval("1s")
                .setExecuteWith(Scheduled.QUARTZ)
                .setTask(se -> {
                    quartzLatch.countDown();
                }).schedule();

        composite.newJob("auto")
                .setInterval("1s")
                .setTask(se -> {
                    autoLatch.countDown();
                }).schedule();

        assertTrue(simpleLatch.await(5, TimeUnit.SECONDS));
        assertTrue(quartzLatch.await(5, TimeUnit.SECONDS));
        assertTrue(autoLatch.await(5, TimeUnit.SECONDS));

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
        reset();
        assertFalse(composite.isRunning());
        assertFalse(simpleLatch.await(2, TimeUnit.SECONDS));

        composite.resume();
        assertTrue(composite.isRunning());
        assertTrue(simpleLatch.await(5, TimeUnit.SECONDS));
        assertTrue(quartzLatch.await(5, TimeUnit.SECONDS));
        assertTrue(autoLatch.await(5, TimeUnit.SECONDS));
    }

}
