package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import io.quarkus.quartz.QuartzScheduler;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusUnitTest;

public class NonconcurrentProgrammaticTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Jobs.class))
            .overrideConfigKey("quarkus.scheduler.start-mode", "halted");

    @Inject
    QuartzScheduler scheduler;

    @Test
    public void testExecution() throws SchedulerException, InterruptedException {
        JobDetail job = JobBuilder.newJob(Jobs.class)
                .withIdentity("foo", Scheduler.class.getName())
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("foo", Scheduler.class.getName())
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(1)
                        .repeatForever())
                .build();
        scheduler.getScheduler().scheduleJob(job, trigger);

        scheduler.resume();

        assertTrue(Jobs.NONCONCURRENT_LATCH.await(10, TimeUnit.SECONDS),
                String.format("nonconcurrent() executed: %sx", Jobs.NONCONCURRENT_COUNTER.get()));
    }

    @DisallowConcurrentExecution
    static class Jobs implements Job {

        static final CountDownLatch NONCONCURRENT_LATCH = new CountDownLatch(1);
        static final CountDownLatch CONCURRENT_LATCH = new CountDownLatch(5);

        static final AtomicInteger NONCONCURRENT_COUNTER = new AtomicInteger(0);

        @Scheduled(identity = "bar", every = "1s")
        void concurrent() throws InterruptedException {
            CONCURRENT_LATCH.countDown();
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            Jobs.NONCONCURRENT_COUNTER.incrementAndGet();
            try {
                if (!Jobs.CONCURRENT_LATCH.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("nonconcurrent() execution blocked too long...");
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            if (Jobs.NONCONCURRENT_COUNTER.get() == 1) {
                // concurrent() executed >= 5x and nonconcurrent() 1x
                Jobs.NONCONCURRENT_LATCH.countDown();
            }
        }

    }

}
