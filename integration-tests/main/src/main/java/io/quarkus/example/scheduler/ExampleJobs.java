package io.quarkus.example.scheduler;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import io.quarkus.scheduler.api.Scheduled;
import io.quarkus.scheduler.api.ScheduledExecution;
import io.quarkus.scheduler.api.Scheduler;

public class ExampleJobs {

    @Inject
    Scheduler scheduler;

    @PostConstruct
    void init() {
        scheduler.startTimer(TimeUnit.SECONDS.toMillis(15), () -> System.out.println("TIMER!"));
    }

    @Scheduled(cron = "0/5 * * * * ?")
    void checkCron() {
        System.out.println("cronFiveSeconds");
    }

    @Scheduled(cron = "{schedulerservice.cron.expr}")
    void checkCronConfig(ScheduledExecution execution) {
        System.out.println(
                "checkCronConfig - scheduled at " + execution.getScheduledFireTime() + ", next fire time: "
                        + execution.getTrigger().getNextFireTime());
    }

    @Scheduled(every = "10s")
    @Scheduled(every = "20s")
    void checkEvery(ScheduledExecution execution) {
        System.out
                .println("everyNSeconds - scheduled at " + execution.getScheduledFireTime() + ", next fire time: "
                        + execution.getTrigger().getNextFireTime());
    }

}
