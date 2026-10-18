package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

import io.quarkus.quartz.QuartzScheduler;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;
import io.quarkus.test.QuarkusExtensionTest;

public class ScheduledDescriptionTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class));

    @Inject
    Scheduler scheduler;

    @Inject
    QuartzScheduler quartzScheduler;

    @Test
    public void testDescriptionViaQuartzApi() throws SchedulerException {
        JobDetail jobDetail = quartzScheduler.getScheduler()
                .getJobDetail(new JobKey("with_description", Scheduler.class.getName()));
        assertEquals("A job that pings", jobDetail.getDescription());
    }

    @Test
    public void testDescriptionViaTrigger() {
        List<Trigger> triggers = scheduler.getScheduledJobs();
        Trigger withDescription = triggers.stream()
                .filter(t -> "with_description".equals(t.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("A job that pings", withDescription.getDescription());

        Trigger withoutDescription = triggers.stream()
                .filter(t -> "without_description".equals(t.getId()))
                .findFirst()
                .orElseThrow();
        assertNull(withoutDescription.getDescription());
    }

    static class Jobs {

        @Scheduled(identity = "with_description", every = "1s", description = "A job that pings")
        void ping() {
        }

        @Scheduled(identity = "without_description", every = "1s")
        void pong() {
        }
    }

}
