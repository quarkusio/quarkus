package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.SchedulerException;

import io.quarkus.quartz.test.listeners.HelloJobListener;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class RegisterJobListenerTest {

    @Inject
    org.quartz.Scheduler quartzScheduler;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addClass(HelloJobListener.class)
                    .addAsResource(new StringAsset(
                            "quarkus.quartz.job-listeners.testJobListener.class=io.quarkus.quartz.test.listeners.HelloJobListener"),
                            "application.properties"));

    @Test
    public void testJobListenerRegistered() throws InterruptedException, SchedulerException {
        assertNotNull(quartzScheduler.getListenerManager().getJobListener("testJobListener"));
    }

    static class Jobs {
        @Scheduled(every = "1s")
        void checkEverySecond() {
            // no op
        }
    }
}
