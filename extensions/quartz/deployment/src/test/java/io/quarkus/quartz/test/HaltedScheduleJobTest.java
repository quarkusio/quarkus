package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Test suite for deploying a job while quartz scheduler in halted state
 */
public final class HaltedScheduleJobTest {

    @SuppressWarnings("unused")
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Starter.class)
                    .addAsResource(new StringAsset("quarkus.quartz.enabled=true\n" +
                            "quarkus.quartz.force-start=true\n" +
                            "quarkus.quartz.halt-start=true"),
                            "application.properties"));
    @Inject
    Instance<org.quartz.Scheduler> quartzScheduler;

    /**
     * Tests whether the job was executed during halted scheduler
     * While halted, no jobs should be executed, after manually starting scheduler,
     * all work should continue
     */
    @Test
    public final void testSchedulerDeployJob() throws InterruptedException, SchedulerException {

        final JobDetail job = JobBuilder.newJob(Starter.class)
                .withIdentity("myJob", "myGroup")
                .build();
        final Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("myTrigger", "myGroup")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(1)
                        .repeatForever())
                .build();
        final org.quartz.Scheduler qScheduler = quartzScheduler.get();
        qScheduler.scheduleJob(job, trigger);
        assertJobExecution(false);
        qScheduler.start();
        assertJobExecution(true);
    }

    /**
     * Asserts if a job was executed or not
     *
     * @param expectedState expected state for the job execution
     */
    private void assertJobExecution(final boolean expectedState) throws InterruptedException {

        assertEquals(expectedState, Starter.LATCH.await(3, TimeUnit.SECONDS),
                "Latch count: " + Starter.LATCH.getCount());
    }

    /**
     * Simple job
     */
    public static class Starter implements Job {

        private static final CountDownLatch LATCH = new CountDownLatch(2);

        @Override
        public final void execute(final JobExecutionContext context) {
            LATCH.countDown();
        }
    }
}