package io.quarkus.it.opentelemetry.quartz;

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
public class FailedManualScheduler {
    @Inject
    org.quartz.Scheduler quartz;

    @PostConstruct
    void init() throws SchedulerException {
        JobDetail job = JobBuilder.newJob(CountingJob.class).withIdentity("myFailedManualJob", "myFailedGroup").build();
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity("myFailedTrigger", "myFailedGroup")
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
            throw new RuntimeException("error occurred in myFailedManualJob.");
        }
    }
}
