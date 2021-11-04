package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;
import io.quarkus.test.QuarkusUnitTest;

public class PropertyDefaultValueSchedulerTest {

    private static final String EXPECTED_IDENTITY = "TestIdentity";

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("PropertyDefaultValueSchedulerTest.default=" + EXPECTED_IDENTITY),
                            "application.properties"));

    @Test
    public void testDefaultIdentity() throws InterruptedException {
        assertTrue(Jobs.LATCH.await(5, TimeUnit.SECONDS), "Scheduler was not triggered");
        assertNotNull(Jobs.execution);
        assertEquals(EXPECTED_IDENTITY, Jobs.execution.getTrigger().getId());
    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(1);

        static volatile ScheduledExecution execution;

        @Scheduled(every = "0.5s", identity = "{nonexistent:${PropertyDefaultValueSchedulerTest.default}}")
        void trigger(ScheduledExecution exec) {
            if (execution == null) {
                execution = exec;
            }
            LATCH.countDown();
        }
    }
}
