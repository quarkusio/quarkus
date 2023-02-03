package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.FailedExecution;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class FailedExecutionTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FailedExecutionTest.Jobs.class));

    static final CountDownLatch ERROR_LATCH = new CountDownLatch(2);
    static FailedExecution failedExecution;

    @Test
    public void testTriggerErrorStatus() throws InterruptedException {
        assertTrue(ERROR_LATCH.await(5, TimeUnit.SECONDS));
        assertInstanceOf(RuntimeException.class, failedExecution.getException());
    }

    void observeFailedExecution(@Observes FailedExecution failedExecution) {
        FailedExecutionTest.failedExecution = failedExecution;
        ERROR_LATCH.countDown();
    }

    static class Jobs {

        @Scheduled(identity = "failing_schedule", every = "0.2s")
        void failingSchedule() {
            throw new RuntimeException("oups");
        }
    }

}
