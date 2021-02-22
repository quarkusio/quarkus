package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class SimpleDelayedTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("jobs.delay=1s"),
                            "application.properties"));

    @Test
    public void testDelayedJobs() throws InterruptedException {
        // Only assert that the scheduled method is executed twice
        assertTrue(Jobs.LATCH.await(5, TimeUnit.SECONDS));
    }

    public static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(4);

        @Scheduled(every = "1s", delay = 1, delayUnit = TimeUnit.SECONDS, delayed = "2h")
        @Scheduled(every = "1s", delayed = "{jobs.delay}")
        void ping() {
            LATCH.countDown();
        }

    }

}
