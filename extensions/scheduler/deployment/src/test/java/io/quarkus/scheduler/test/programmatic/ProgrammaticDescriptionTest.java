package io.quarkus.scheduler.test.programmatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.quarkus.test.QuarkusExtensionTest;

public class ProgrammaticDescriptionTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class));

    @Inject
    Scheduler scheduler;

    static final CountDownLatch WITH_DESC_LATCH = new CountDownLatch(1);
    static final CountDownLatch WITHOUT_DESC_LATCH = new CountDownLatch(1);

    @Test
    public void testProgrammaticDescription() throws InterruptedException {
        Trigger withDesc = scheduler.newJob("withDescription")
                .setInterval("1s")
                .setDescription("My programmatic job description")
                .setTask(ec -> WITH_DESC_LATCH.countDown())
                .schedule();
        assertNotNull(withDesc);
        assertEquals("My programmatic job description", withDesc.getDescription());

        Trigger withoutDesc = scheduler.newJob("withoutDescription")
                .setInterval("1s")
                .setTask(ec -> WITHOUT_DESC_LATCH.countDown())
                .schedule();
        assertNotNull(withoutDesc);
        assertNull(withoutDesc.getDescription());

        WITH_DESC_LATCH.await(5, TimeUnit.SECONDS);
        WITHOUT_DESC_LATCH.await(5, TimeUnit.SECONDS);

        scheduler.unscheduleJob("withDescription");
        scheduler.unscheduleJob("withoutDescription");
    }

    static class Jobs {

        @Scheduled(identity = "dummy", every = "60m")
        static void dummy() {
        }
    }

}
