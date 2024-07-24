package io.quarkus.quartz.test.programmatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.UnableToInterruptJobException;

import io.quarkus.test.QuarkusUnitTest;

public class InterruptableJobTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyJob.class)
                    .addAsResource(new StringAsset("quarkus.scheduler.start-mode=forced"),
                            "application.properties"));

    @Inject
    Scheduler scheduler;

    static final CountDownLatch INTERRUPT_LATCH = new CountDownLatch(1);
    static final CountDownLatch EXECUTE_LATCH = new CountDownLatch(1);
    static final CountDownLatch DESTROY_LATCH = new CountDownLatch(1);

    static final CountDownLatch NON_INTERRUPTABLE_EXECUTE_LATCH = new CountDownLatch(1);
    static final CountDownLatch NON_INTERRUPTABLE_HOLD_LATCH = new CountDownLatch(1);

    @Test
    public void testInterruptableJob() throws InterruptedException {
        assertEquals(0, MyJob.timesInitialized);
        assertEquals(0, MyJob.timesPredestroyInvoked);

        String jobKey = "myJob";
        JobKey key = new JobKey(jobKey);
        Trigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .build();

        JobDetail job = JobBuilder.newJob(MyJob.class)
                .withIdentity(key)
                .build();

        try {
            scheduler.scheduleJob(job, trigger);
            // wait for job to start executing, then interrupt
            EXECUTE_LATCH.await(2, TimeUnit.SECONDS);
            assertEquals(1, MyJob.timesInitialized);
            scheduler.interrupt(key);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        assertTrue(INTERRUPT_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(DESTROY_LATCH.await(3, TimeUnit.SECONDS));
        assertEquals(1, MyJob.timesInitialized);
        assertEquals(1, MyJob.timesPredestroyInvoked);
    }

    @Test
    public void testNonInterruptableJob() throws InterruptedException {

        String jobKey = "myNonInterruptableJob";
        JobKey key = new JobKey(jobKey);
        Trigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .build();

        JobDetail job = JobBuilder.newJob(MyNonInterruptableJob.class)
                .withIdentity(key)
                .build();

        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        // wait for job to start executing, then interrupt
        NON_INTERRUPTABLE_EXECUTE_LATCH.await(2, TimeUnit.SECONDS);
        try {
            scheduler.interrupt(key);
            fail("Should have thrown UnableToInterruptJobException");
        } catch (UnableToInterruptJobException e) {
            // This is expected, release the latch holding the job
            NON_INTERRUPTABLE_HOLD_LATCH.countDown();
        }
    }

    @Dependent
    static class MyJob implements InterruptableJob {

        public static int timesInitialized = 0;
        public static int timesPredestroyInvoked = 0;

        public MyJob() {
            timesInitialized++;
        }

        @PreDestroy
        public void destroy() {
            timesPredestroyInvoked++;
            DESTROY_LATCH.countDown();
        }

        @Override
        public void execute(JobExecutionContext context) {
            EXECUTE_LATCH.countDown();
            try {
                // halt execution so that we can interrupt it
                INTERRUPT_LATCH.await(4, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void interrupt() {
            INTERRUPT_LATCH.countDown();
        }
    }

    @ApplicationScoped
    static class MyNonInterruptableJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            NON_INTERRUPTABLE_EXECUTE_LATCH.countDown();
            try {
                // halt execution so that we can interrupt it
                NON_INTERRUPTABLE_HOLD_LATCH.await(4, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
