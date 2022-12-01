package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class RegisterLoggingJobHistoryPluginTest {

    @Inject
    Scheduler quartzScheduler;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset(
                            "quarkus.quartz.plugin.jobHistory.class=org.quartz.plugins.history.LoggingJobHistoryPlugin\n"
                                    + "quarkus.quartz.plugin.jobHistory.jobSuccessMessage=Job [{1}.{0}] execution complete and reports: {8}"),
                            "application.properties"));

    @Test
    public void testSchedulerStarted() throws InterruptedException {
        assertTrue(quartzScheduler.isRunning());
    }

    static class Jobs {
        @Scheduled(every = "1s")
        void checkEverySecond() {
            // no op
        }
    }
}
