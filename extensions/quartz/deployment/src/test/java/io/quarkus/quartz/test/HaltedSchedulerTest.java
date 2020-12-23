package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.SchedulerException;

import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Test suite for property halt-start on quartz scheduler
 */
public final class HaltedSchedulerTest {

    @SuppressWarnings("unused")
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("quarkus.quartz.enabled=true\n" +
                            "quarkus.quartz.force-start=true\n" +
                            "quarkus.quartz.halt-start=true"),
                            "application.properties"));
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    Scheduler scheduler;
    @Inject
    Instance<org.quartz.Scheduler> quartzScheduler;

    /**
     * Tests scheduler halted state
     */
    @Test
    public final void testSchedulerHalted() throws SchedulerException {
        assertSchedulerState(false);
        quartzScheduler.get().start();
        assertSchedulerState(true);
    }

    /**
     * Asserts scheduler in a given state
     *
     * @param expectedState expected state where scheduler should be in
     */
    private void assertSchedulerState(final boolean expectedState) throws SchedulerException {
        assertEquals(expectedState, scheduler.isRunning());
        assertEquals(expectedState, quartzScheduler.get().isStarted());
    }
}