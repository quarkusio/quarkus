package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.quarkus.test.QuarkusUnitTest;

public class ListSchedulerJobsTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ListSchedulerJobsTest.Jobs.class));

    @Inject
    Scheduler scheduler;

    @Test
    public void testSchedulerListScheduledJobsMethod() {
        List<Trigger> triggers = scheduler.getScheduledJobs();
        assertEquals(triggers.size(), 1);
        Trigger trigger = triggers.get(0);
        assertEquals("the_schedule", trigger.getId());
    }

    static class Jobs {

        @Scheduled(identity = "the_schedule", every = "1s")
        void ping() {
        }
    }

}
