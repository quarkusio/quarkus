package io.quarkus.it.quartz;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

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

    @Transactional
    @PostConstruct
    void init() throws SchedulerException {
        JobDetail job = JobBuilder.newJob(CountingJob.class).build();
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .startNow()
                .withSchedule(SimpleScheduleBuilder
                        .simpleSchedule()
                        .withIntervalInSeconds(1))
                .build();
        quartz.scheduleJob(job, trigger);
    }

    @RegisterForReflection
    public static class CountingJob implements Job {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) {
            counter.incrementAndGet();
        }
    }
}
