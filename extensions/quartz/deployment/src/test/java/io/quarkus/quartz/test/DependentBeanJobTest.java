package io.quarkus.quartz.test;

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
                    .addClasses(Service.class, MyJob.class, RefiringJob.class)
                    .addAsResource(new StringAsset("quarkus.scheduler.start-mode=forced"),
                            "application.properties"));

    @Inject
    Scheduler quartz;

    @Inject
    Service service;

    @Test
    public void testDependentBeanJobDestroyed() throws SchedulerException, InterruptedException {
        // prepare latches, schedule 10 one-off jobs, assert
        CountDownLatch execLatch = service.initExecuteLatch(10);
        CountDownLatch constructLatch = service.initConstructLatch(10);
        CountDownLatch destroyedLatch = service.initDestroyedLatch(10);
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
        assertTrue(execLatch.await(2, TimeUnit.SECONDS), "Latch count: " + execLatch.getCount());
        assertTrue(constructLatch.await(2, TimeUnit.SECONDS), "Latch count: " + constructLatch.getCount());
        assertTrue(destroyedLatch.await(2, TimeUnit.SECONDS), "Latch count: " + destroyedLatch.getCount());

        // now try the same with repeating job triggering three times
        execLatch = service.initExecuteLatch(3);
        constructLatch = service.initConstructLatch(3);
        destroyedLatch = service.initDestroyedLatch(3);
        JobDetail job = JobBuilder.newJob(MyJob.class)
                .withIdentity("myRepeatingJob", "myGroup")
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("myRepeatingTrigger", "myGroup")
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInMilliseconds(333)
                                .withRepeatCount(2))
                .build();
        quartz.scheduleJob(job, trigger);

        assertTrue(execLatch.await(2, TimeUnit.SECONDS), "Latch count: " + execLatch.getCount());
        assertTrue(constructLatch.await(2, TimeUnit.SECONDS), "Latch count: " + constructLatch.getCount());
        assertTrue(destroyedLatch.await(2, TimeUnit.SECONDS), "Latch count: " + destroyedLatch.getCount());
    }

    @Test
    public void testDependentBeanJobWithRefire() throws SchedulerException, InterruptedException {
        // 5 one-off jobs should trigger construction/execution/destruction 5 times in total
        CountDownLatch execLatch = service.initExecuteLatch(5);
        CountDownLatch constructLatch = service.initConstructLatch(5);
        CountDownLatch destroyedLatch = service.initDestroyedLatch(5);
        for (int i = 0; i < 5; i++) {
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("myTrigger" + i, "myRefiringGroup")
                    .startNow()
                    .build();
            JobDetail job = JobBuilder.newJob(RefiringJob.class)
                    .withIdentity("myJob" + i, "myRefiringGroup")
                    .build();
            quartz.scheduleJob(job, trigger);
        }
        assertTrue(execLatch.await(2, TimeUnit.SECONDS), "Latch count: " + execLatch.getCount());
        assertTrue(constructLatch.await(2, TimeUnit.SECONDS), "Latch count: " + constructLatch.getCount());
        assertTrue(destroyedLatch.await(2, TimeUnit.SECONDS), "Latch count: " + destroyedLatch.getCount());

        // repeating job triggering three times; re-fires should NOT recreate the bean instance
        execLatch = service.initExecuteLatch(3);
        constructLatch = service.initConstructLatch(3);
        destroyedLatch = service.initDestroyedLatch(3);
        JobDetail job = JobBuilder.newJob(RefiringJob.class)
                .withIdentity("myRepeatingJob", "myRefiringGroup")
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("myRepeatingTrigger", "myRefiringGroup")
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInMilliseconds(333)
                                .withRepeatCount(2))
                .build();
        quartz.scheduleJob(job, trigger);

        assertTrue(execLatch.await(2, TimeUnit.SECONDS), "Latch count: " + execLatch.getCount());
        assertTrue(constructLatch.await(2, TimeUnit.SECONDS), "Latch count: " + constructLatch.getCount());
        assertTrue(destroyedLatch.await(2, TimeUnit.SECONDS), "Latch count: " + destroyedLatch.getCount());
    }

    @ApplicationScoped
    public static class Service {

        volatile CountDownLatch executeLatch;
        volatile CountDownLatch constructedLatch;
        volatile CountDownLatch destroyedLatch;

        public CountDownLatch initExecuteLatch(int latchCountdown) {
            this.executeLatch = new CountDownLatch(latchCountdown);
            return executeLatch;
        }

        public CountDownLatch initConstructLatch(int latchCountdown) {
            this.constructedLatch = new CountDownLatch(latchCountdown);
            return constructedLatch;
        }

        public CountDownLatch initDestroyedLatch(int latchCountdown) {
            this.destroyedLatch = new CountDownLatch(latchCountdown);
            return destroyedLatch;
        }

        public void execute() {
            executeLatch.countDown();
        }

        public void constructedLatch() {
            constructedLatch.countDown();
        }

        public void destroyedLatch() {
            destroyedLatch.countDown();
        }

    }

    @Dependent
    static class MyJob implements Job {

        @Inject
        Service service;

        @PostConstruct
        void postConstruct() {
            service.constructedLatch();
        }

        @PreDestroy
        void preDestroy() {
            service.destroyedLatch();
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            service.execute();
        }
    }

    @Dependent
    static class RefiringJob implements Job {

        @Inject
        Service service;

        @PostConstruct
        void postConstruct() {
            service.constructedLatch();
        }

        @PreDestroy
        void preDestroy() {
            service.destroyedLatch();
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            if (context.getRefireCount() == 0) {
                service.execute();
                // request re-fire; we expect a new dependent bean to be used for that
                throw new JobExecutionException("Refiring job", true);
            } else {
                service.execute();
                // no re-fire the second time
                throw new JobExecutionException("Job was re-fired successfully", false);
            }
        }
    }
}
