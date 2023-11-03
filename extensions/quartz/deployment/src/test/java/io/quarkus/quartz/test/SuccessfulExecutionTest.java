package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.SuccessfulExecution;
import io.quarkus.test.QuarkusUnitTest;

public class SuccessfulExecutionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SuccessfulExecutionTest.Jobs.class));

    static final CountDownLatch SUCCESS_LATCH = new CountDownLatch(2);
    static SuccessfulExecution successfulExecution;

    @Test
    public void testTriggerErrorStatus() throws InterruptedException {
        assertTrue(SUCCESS_LATCH.await(5, TimeUnit.SECONDS));
    }

    void observeFailedExecution(@Observes SuccessfulExecution successfulExecution) {
        SuccessfulExecutionTest.successfulExecution = successfulExecution;
        SUCCESS_LATCH.countDown();
    }

    static class Jobs {

        @Scheduled(identity = "successful_schedule", every = "0.2s")
        void successfulSchedule() {
        }
    }
}
