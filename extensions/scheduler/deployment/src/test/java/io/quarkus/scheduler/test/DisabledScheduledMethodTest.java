package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class DisabledScheduledMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("DisabledScheduledMethodTest.interval=off"),
                            "application.properties"));

    @Test
    public void testNoSchedulerInvocations() throws InterruptedException {
        assertTrue(Jobs.LATCH.await(2, TimeUnit.SECONDS));
        assertEquals(0, Jobs.executionCounter);
    }

    static class Jobs {

        static final CountDownLatch LATCH = new CountDownLatch(1);

        static volatile int executionCounter = 0;

        @Scheduled(every = "{DisabledScheduledMethodTest.interval}")
        void disabledByConfigValue() {
            executionCounter++;
        }

        @Scheduled(every = "{non.existent.property:disabled}")
        void disabledByDefault() {
            executionCounter++;
        }

        @Scheduled(every = "0.5s")
        void enabled() {
            LATCH.countDown();
        }
    }
}
