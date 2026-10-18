package io.quarkus.it.opentelemetry.quartz;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import io.quarkus.runtime.Startup;
import io.quarkus.runtime.annotations.RegisterForReflection;

@Startup
@ApplicationScoped
public class ManualScheduledCounter {
    @Inject
    org.quartz.Scheduler quartz;
    private static AtomicInteger counter = new AtomicInteger();

    public int get() {
        return counter.get();
    }

    @PostConstruct
    void init() throws SchedulerException {
        JobDetail job = JobBuilder.newJob(CountingJob.class).withIdentity("myManualJob", "myGroup").build();
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity("myTrigger", "myGroup")
                .startNow()
                .withSchedule(SimpleScheduleBuilder
                        .simpleSchedule()
                        .repeatForever()
                        .withIntervalInSeconds(1))
                .build();
        quartz.scheduleJob(job, trigger);
    }

    @RegisterForReflection
    public static class CountingJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            try {
                Thread.sleep(100l);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            counter.incrementAndGet();
        }
    }
}
