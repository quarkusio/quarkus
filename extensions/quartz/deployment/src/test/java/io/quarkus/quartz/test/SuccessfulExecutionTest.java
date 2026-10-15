package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.StartedExecution;
import io.quarkus.scheduler.SuccessfulExecution;
import io.quarkus.test.QuarkusExtensionTest;

public class SuccessfulExecutionTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SuccessfulExecutionTest.Jobs.class));

    static final CountDownLatch SUCCESS_LATCH = new CountDownLatch(2);
    static final CountDownLatch STARTED_LATCH = new CountDownLatch(2);
    static final List<String> EVENT_ORDER = new CopyOnWriteArrayList<>();
    static SuccessfulExecution successfulExecution;
    static StartedExecution startedExecution;

    @Test
    public void testTriggerErrorStatus() throws InterruptedException {
        assertTrue(STARTED_LATCH.await(5, TimeUnit.SECONDS));
        assertNotNull(startedExecution.getExecution());
        assertTrue(SUCCESS_LATCH.await(5, TimeUnit.SECONDS));
        assertNotNull(successfulExecution.getExecution());
        assertTrue(EVENT_ORDER.indexOf("started") < EVENT_ORDER.indexOf("success"),
                "StartedExecution must fire before SuccessfulExecution");
    }

    void observeSuccessfulExecution(@Observes SuccessfulExecution successfulExecution) {
        EVENT_ORDER.add("success");
        SuccessfulExecutionTest.successfulExecution = successfulExecution;
        SUCCESS_LATCH.countDown();
    }

    void observeStartedExecution(@Observes StartedExecution startedExecution) {
        EVENT_ORDER.add("started");
        SuccessfulExecutionTest.startedExecution = startedExecution;
        STARTED_LATCH.countDown();
    }

    static class Jobs {

        @Scheduled(identity = "successful_schedule", every = "0.2s")
        void successfulSchedule() {
        }
    }
}
