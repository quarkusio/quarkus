package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Priority;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.FailedExecution;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.SuccessfulExecution;
import io.quarkus.test.QuarkusUnitTest;

public class ApplicationNotRunningPredicateTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClasses(Jobs.class));

    static final CountDownLatch SUCCESS_LATCH = new CountDownLatch(1);
    static volatile FailedExecution failedExecution;

    @Test
    public void testTriggerErrorStatus() throws InterruptedException {
        assertTrue(SUCCESS_LATCH.await(5, TimeUnit.SECONDS));
        assertNull(failedExecution);
    }

    void observeSuccessfulExecution(@Observes SuccessfulExecution successfulExecution) {
        SUCCESS_LATCH.countDown();
    }

    void observeFailedExecution(@Observes FailedExecution failedExecution) {
        ApplicationNotRunningPredicateTest.failedExecution = failedExecution;
    }

    static class Jobs {

        volatile boolean preStart;

        void started(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) StartupEvent event) {
            preStart = true;
        }

        @Scheduled(every = "0.2s", skipExecutionIf = Scheduled.ApplicationNotRunning.class)
        void scheduleAfterStarted() {
            if (!preStart) {
                throw new IllegalStateException();
            }
        }
    }
}
