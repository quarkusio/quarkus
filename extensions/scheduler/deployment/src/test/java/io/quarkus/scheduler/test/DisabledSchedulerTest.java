package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class DisabledSchedulerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("quarkus.scheduler.enabled=false"),
                            "application.properties"));

    @Inject
    Scheduler scheduler;

    @Test
    public void testNoSchedulerInvocations() throws InterruptedException {
        assertFalse(scheduler.isRunning());
    }

    static class Jobs {
        @Scheduled(every = "1s")
        void checkEverySecond() {
            // no op
        }
    }
}
