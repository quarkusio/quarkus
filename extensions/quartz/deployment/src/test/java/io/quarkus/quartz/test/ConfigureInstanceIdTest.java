package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigureInstanceIdTest {

    @Inject
    Scheduler quartzScheduler;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClasses(Jobs.class)
            .addAsResource(new StringAsset("quarkus.quartz.instance-id=myInstanceId"), "application.properties"));

    @Test
    public void testSchedulerStarted() throws SchedulerException {
        assertEquals("myInstanceId", quartzScheduler.getSchedulerInstanceId());
    }

    static class Jobs {
        @Scheduled(every = "1s")
        void checkEverySecond() {
            // no op
        }
    }

}
