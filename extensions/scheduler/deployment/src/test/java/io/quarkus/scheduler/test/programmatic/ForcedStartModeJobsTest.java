package io.quarkus.scheduler.test.programmatic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class ForcedStartModeJobsTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("quarkus.scheduler.start-mode=forced"),
                            "application.properties"));

    @Inject
    Scheduler scheduler;

    static final CountDownLatch SYNC_LATCH = new CountDownLatch(1);

    @Test
    public void testScheduler() throws InterruptedException {
        assertTrue(scheduler.isRunning());

        scheduler.newJob("foo")
                .setInterval("1s")
                .setTask(ec -> ForcedStartModeJobsTest.SYNC_LATCH.countDown())
                .schedule();

        assertTrue(ForcedStartModeJobsTest.SYNC_LATCH.await(5, TimeUnit.SECONDS));
    }

}
