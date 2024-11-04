package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class DisabledSchedulerTest {

    @Inject
    Scheduler scheduler;

    @Inject
    Instance<org.quartz.Scheduler> quartzScheduler;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("quarkus.scheduler.enabled=false"),
                            "application.properties"));

    @Test
    public void testSchedulerInvocations() throws InterruptedException {
        assertNotNull(scheduler);
        assertFalse(scheduler.isStarted());
        assertFalse(scheduler.isRunning());
        assertNotNull(scheduler.implementation());
        assertThrows(UnsupportedOperationException.class, () -> scheduler.newJob("foo"));
        assertThrows(UnsupportedOperationException.class, () -> scheduler.unscheduleJob("foo"));
        assertThrows(UnsupportedOperationException.class, () -> scheduler.pause());
        assertThrows(UnsupportedOperationException.class, () -> scheduler.pause("foo"));
        assertThrows(UnsupportedOperationException.class, () -> scheduler.resume());
        assertThrows(UnsupportedOperationException.class, () -> scheduler.resume("foo"));
        assertThrows(UnsupportedOperationException.class, () -> scheduler.getScheduledJobs());
        assertThrows(UnsupportedOperationException.class, () -> scheduler.getScheduledJob("bar"));
    }

    static class Jobs {
        @Scheduled(every = "1s")
        void checkEverySecond() {
            // no op
        }
    }
}
