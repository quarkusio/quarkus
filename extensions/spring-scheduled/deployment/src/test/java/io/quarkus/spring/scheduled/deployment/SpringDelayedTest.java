package io.quarkus.spring.scheduled.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.scheduling.annotation.Scheduled;

import io.quarkus.test.QuarkusUnitTest;

public class SpringDelayedTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset(
                            "springScheduledSimpleJobs.fixedRate=1000\nspringScheduledSimpleJobs.initialDelay=1000"),
                            "application.properties"));

    @Test
    public void testDelayedJobs() throws InterruptedException {
        // Only assert that the scheduled method is executed twice
        assertTrue(Jobs.LATCH.await(5, TimeUnit.SECONDS));
    }

    @ApplicationScoped
    public static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(4);

        @Scheduled(fixedRate = 1000, initialDelay = 1000)
        @Scheduled(fixedRateString = "${springScheduledSimpleJobs.fixedRate}", initialDelayString = "${springScheduledSimpleJobs.initialDelay}")
        void ping() {
            LATCH.countDown();
        }

    }

}
