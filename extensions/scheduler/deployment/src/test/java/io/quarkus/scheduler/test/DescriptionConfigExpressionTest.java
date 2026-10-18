package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import io.quarkus.test.QuarkusExtensionTest;

public class DescriptionConfigExpressionTest {

    private static final String EXPECTED_DESCRIPTION = "Resolved description";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("myJob.description=" + EXPECTED_DESCRIPTION),
                            "application.properties"));

    @Inject
    Scheduler scheduler;

    @Test
    public void testConfigExpressionDescription() throws InterruptedException {
        assertTrue(Jobs.LATCH.await(5, TimeUnit.SECONDS), "Scheduler was not triggered");
        Trigger trigger = scheduler.getScheduledJob("myConfigDescJob");
        assertNotNull(trigger);
        assertEquals(EXPECTED_DESCRIPTION, trigger.getDescription());
    }

    @Test
    public void testDefaultValueDescription() throws InterruptedException {
        assertTrue(Jobs.DEFAULT_LATCH.await(5, TimeUnit.SECONDS), "Scheduler was not triggered");
        Trigger trigger = scheduler.getScheduledJob("myDefaultDescJob");
        assertNotNull(trigger);
        assertEquals("Default description", trigger.getDescription());
    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(1);
        static final CountDownLatch DEFAULT_LATCH = new CountDownLatch(1);

        @Scheduled(identity = "myConfigDescJob", every = "1s", description = "${myJob.description}")
        void configDescription() {
            LATCH.countDown();
        }

        @Scheduled(identity = "myDefaultDescJob", every = "1s", description = "${myJob.nonexistent:Default description}")
        void defaultDescription() {
            DEFAULT_LATCH.countDown();
        }
    }
}
