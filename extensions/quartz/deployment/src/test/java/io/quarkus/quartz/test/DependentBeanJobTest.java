package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import io.quarkus.test.QuarkusUnitTest;

public class DependentBeanJobTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Service.class, MyJob.class)
                    .addAsResource(new StringAsset("quarkus.quartz.start-mode=forced"),
                            "application.properties"));

    @Inject
    Scheduler quartz;

    @Inject
    Service service;

    @Test
    public void testDependentBeanJobDestroyed() throws SchedulerException, InterruptedException {
        assertEquals(0, MyJob.timesConstructed);
        assertEquals(0, MyJob.timesDestroyed);
        // prepare latch, schedule 10 one-off jobs, assert
        CountDownLatch latch = service.initializeLatch(10);
        for (int i = 0; i < 10; i++) {
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("myTrigger" + i, "myGroup")
                    .startNow()
                    .build();
            JobDetail job = JobBuilder.newJob(MyJob.class)
                    .withIdentity("myJob" + i, "myGroup")
                    .build();
            quartz.scheduleJob(job, trigger);
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Latch count: " + latch.getCount());
        assertEquals(10, MyJob.timesConstructed);
        assertEquals(10, MyJob.timesDestroyed);

        // now try the same with repeating job triggering three times
        latch = service.initializeLatch(3);
        JobDetail job = JobBuilder.newJob(MyJob.class)
                .withIdentity("myRepeatingJob", "myGroup")
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("myRepeatingTrigger", "myGroup")
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInMilliseconds(333)
                                .withRepeatCount(3))
                .build();
        quartz.scheduleJob(job, trigger);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Latch count: " + latch.getCount());
        assertEquals(13, MyJob.timesConstructed);
        assertEquals(13, MyJob.timesDestroyed);
    }

    @ApplicationScoped
    public static class Service {

        volatile CountDownLatch latch;

        public CountDownLatch initializeLatch(int latchCountdown) {
            this.latch = new CountDownLatch(latchCountdown);
            return latch;
        }

        public void execute() {
            latch.countDown();
        }

    }

    @Dependent
    static class MyJob implements Job {

        public static volatile int timesConstructed = 0;
        public static volatile int timesDestroyed = 0;

        @Inject
        Service service;

        @PostConstruct
        void postConstruct() {
            timesConstructed++;
        }

        @PreDestroy
        void preDestroy() {
            timesDestroyed++;
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            service.execute();
        }
    }
}
