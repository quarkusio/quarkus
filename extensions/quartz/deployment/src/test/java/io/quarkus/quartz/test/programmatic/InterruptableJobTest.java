package io.quarkus.quartz.test.programmatic;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
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
    static Integer initCounter = 0;

    static final CountDownLatch NON_INTERRUPTABLE_EXECUTE_LATCH = new CountDownLatch(1);
    static final CountDownLatch NON_INTERRUPTABLE_HOLD_LATCH = new CountDownLatch(1);

    @Test
    public void testInterruptableJob() throws InterruptedException {

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
            scheduler.interrupt(key);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        assertTrue(INTERRUPT_LATCH.await(3, TimeUnit.SECONDS));
        // asserts that a single dep. scoped bean instance was used for both, execute() and interrupt() methods
        assertTrue(initCounter == 1);
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

    @ApplicationScoped
    static class MyJob implements InterruptableJob {

        @PostConstruct
        public void postConstruct() {
            initCounter++;
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
