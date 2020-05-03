package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class DisabledSchedulerTest {

    @Inject
    Scheduler quartzScheduler;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Jobs.class)
                    .addAsResource(new StringAsset("quarkus.scheduler.enabled=false"),
                            "application.properties"));

    @Test
    public void testNoSchedulerInvocations() throws InterruptedException {
        assertFalse(quartzScheduler.isRunning());
    }

    static class Jobs {
        @Scheduled(every = "1s")
        void checkEverySecond() {
            // no op
        }
    }
}
