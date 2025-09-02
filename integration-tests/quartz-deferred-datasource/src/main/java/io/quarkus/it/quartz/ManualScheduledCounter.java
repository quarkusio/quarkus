package io.quarkus.it.quartz;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
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
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("firetime", new CounterFireTime());
        jobDataMap.put("long", 1L);
        jobDataMap.put("int", Integer.valueOf(90));
        jobDataMap.put("string", "string value");

        JobDetail job = JobBuilder.newJob(CountingJob.class).storeDurably().setJobData(jobDataMap).build();
        Trigger trigger = TriggerBuilder
                .newTrigger()
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
            counter.incrementAndGet();
        }
    }

    // This class is here to test that complex job data objects can be properly serialized
    public static class CounterFireTime implements Serializable {

        private static final long serialVersionUID = 7523966565034938905L;

        public Date date;

        public CounterFireTime(Date date) {
            this.date = date;
        }

        public CounterFireTime() {
            this.date = Date.from(Instant.now());
        }
    }

    @RegisterForReflection(serialization = true, targets = { CounterFireTime.class, Number.class, Date.class, Long.class,
            Integer.class })
    public static class RegisterForSerializationConfiguration {
    }
}
