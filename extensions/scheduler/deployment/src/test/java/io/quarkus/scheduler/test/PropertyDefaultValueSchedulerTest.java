package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.test.QuarkusUnitTest;

public class PropertyDefaultValueSchedulerTest {

    private static final String EXPECTED_IDENTITY = "TestIdentity";

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("PropertyDefaultValueSchedulerTest.default=" + EXPECTED_IDENTITY),
                            "application.properties"));

    @Test
    public void testDefaultIdentity() throws InterruptedException {
        assertTrue(Jobs.LATCH.await(500, TimeUnit.MILLISECONDS), "Scheduler was not triggered");
        assertNotNull(Jobs.execution);
        assertEquals(EXPECTED_IDENTITY, Jobs.execution.getTrigger().getId());
    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(1);

        static ScheduledExecution execution;

        @Scheduled(every = "0.001s", identity = "{nonexistent:${PropertyDefaultValueSchedulerTest.default}}")
        void trigger(ScheduledExecution execution) {
            if (this.execution == null) {
                this.execution = execution;
            }
            LATCH.countDown();
        }
    }
}
