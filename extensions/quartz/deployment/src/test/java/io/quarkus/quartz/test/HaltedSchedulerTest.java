package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.SchedulerException;

import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public final class HaltedSchedulerTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.quartz.start-mode=halted"),
                            "application.properties"));

    @Inject
    Scheduler scheduler;

    @Inject
    org.quartz.Scheduler quartzScheduler;

    @Test
    public final void testSchedulerHalted() throws SchedulerException {
        assertSchedulerState(false);
        quartzScheduler.start();
        assertSchedulerState(true);
    }

    private void assertSchedulerState(final boolean expectedState) throws SchedulerException {
        assertEquals(expectedState, scheduler.isRunning());
        assertEquals(expectedState, quartzScheduler.isStarted());
    }
}
