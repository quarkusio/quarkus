package io.quarkus.quartz.test.programmatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class HaltedStartModeJobsTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot(root -> root
            .addAsResource(new StringAsset("quarkus.scheduler.start-mode=halted"), "application.properties"));

    @Inject
    Scheduler scheduler;

    static final CountDownLatch SYNC_LATCH = new CountDownLatch(1);

    @Test
    public void testScheduler() throws InterruptedException {
        assertFalse(scheduler.isRunning());

        scheduler.newJob("foo").setInterval("1s").setTask(ec -> HaltedStartModeJobsTest.SYNC_LATCH.countDown())
                .schedule();

        scheduler.resume();
        assertTrue(scheduler.isRunning());
        assertTrue(HaltedStartModeJobsTest.SYNC_LATCH.await(5, TimeUnit.SECONDS));
    }

}
