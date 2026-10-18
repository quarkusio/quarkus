package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.FailedExecution;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.StartedExecution;
import io.quarkus.test.QuarkusExtensionTest;

public class FailedExecutionTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FailedExecutionTest.Jobs.class));

    static final CountDownLatch ERROR_LATCH = new CountDownLatch(2);
    static final CountDownLatch STARTED_LATCH = new CountDownLatch(2);
    static final List<String> EVENT_ORDER = new CopyOnWriteArrayList<>();
    static FailedExecution failedExecution;
    static StartedExecution startedExecution;

    @Test
    public void testTriggerErrorStatus() throws InterruptedException {
        assertTrue(STARTED_LATCH.await(5, TimeUnit.SECONDS));
        assertNotNull(startedExecution.getExecution());
        assertTrue(ERROR_LATCH.await(5, TimeUnit.SECONDS));
        assertInstanceOf(RuntimeException.class, failedExecution.getException());
        assertTrue(EVENT_ORDER.indexOf("started") < EVENT_ORDER.indexOf("failed"),
                "StartedExecution must fire before FailedExecution");
    }

    void observeFailedExecution(@Observes FailedExecution failedExecution) {
        EVENT_ORDER.add("failed");
        FailedExecutionTest.failedExecution = failedExecution;
        ERROR_LATCH.countDown();
    }

    void observeStartedExecution(@Observes StartedExecution startedExecution) {
        EVENT_ORDER.add("started");
        FailedExecutionTest.startedExecution = startedExecution;
        STARTED_LATCH.countDown();
    }

    static class Jobs {

        @Scheduled(identity = "failing_schedule", every = "0.2s")
        void failingSchedule() {
            throw new RuntimeException("oups");
        }
    }

}
