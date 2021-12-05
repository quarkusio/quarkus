package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.event.Observes;

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

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;

public class InjectQuartzSchedulerTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Starter.class)
                    .addAsResource(new StringAsset("quarkus.quartz.start-mode=forced"),
                            "application.properties"));

    @Test
    public void testSimpleScheduledJobs() throws InterruptedException {
        assertTrue(Starter.LATCH.await(5, TimeUnit.SECONDS), "Latch count: " + Starter.LATCH.getCount());
    }

    public static class Starter implements Job {

        static final CountDownLatch LATCH = new CountDownLatch(2);

        void onStart(@Observes StartupEvent event, Scheduler quartz) throws SchedulerException {
            JobDetail job = JobBuilder.newJob(Starter.class)
                    .withIdentity("myJob", "myGroup")
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("myTrigger", "myGroup")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(1)
                            .repeatForever())
                    .build();
            quartz.scheduleJob(job, trigger);
        }

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            LATCH.countDown();
        }

    }

}
