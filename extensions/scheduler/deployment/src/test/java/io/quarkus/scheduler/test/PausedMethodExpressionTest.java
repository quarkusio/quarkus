package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class PausedMethodExpressionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PausedSchedulerTest.Jobs.class)
                    .addAsResource(new StringAsset("scheduler.identity=myIdentity"),
                            "application.properties"));

    @Inject
    Scheduler scheduler;

    private static final String IDENTITY = "{scheduler.identity}";

    @Test
    public void testSchedulerPauseMethod() throws InterruptedException {
        scheduler.pause(IDENTITY);
        assertFalse(PausedMethodExpressionTest.Jobs.LATCH.await(3, TimeUnit.SECONDS));
        scheduler.resume(IDENTITY);
        assertTrue(PausedMethodExpressionTest.Jobs.LATCH.await(2, TimeUnit.SECONDS));
    }

    static class Jobs {
        static final CountDownLatch LATCH = new CountDownLatch(2);

        @Scheduled(every = "1s", identity = IDENTITY)
        void countDownSecond() {
            LATCH.countDown();
        }
    }
}
