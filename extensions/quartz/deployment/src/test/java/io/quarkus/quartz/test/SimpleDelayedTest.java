package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class SimpleDelayedTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("jobs.delay=1s"),
                            "application.properties"));

    @Test
    public void testDelayedJobs() throws InterruptedException {
        // Only assert that the scheduled method is executed twice
        assertTrue(Jobs.LATCH.await(5, TimeUnit.SECONDS));
    }

    public static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(2);

        @Scheduled(every = "1s", delay = 1, delayUnit = TimeUnit.SECONDS)
        @Scheduled(every = "1s", delayed = "{jobs.delay}")
        void ping() {
            LATCH.countDown();
        }

    }

}
