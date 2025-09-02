package io.quarkus.it.quartz;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class DisabledScheduledMethods {
    volatile static String valueSetByCronScheduledMethod = "";
    volatile static String valueSetByEveryScheduledMethod = "";

    // This should never be called as the job is disabled
    @Scheduled(cron = "${disabled}", identity = "disabled-cron-counter")
    void setValueByCron() {
        valueSetByCronScheduledMethod = "cron";
    }

    // This should never be called as the job is turned off
    @Scheduled(every = "${off}", identity = "disabled-every-counter")
    void setValueByEvery() {
        valueSetByEveryScheduledMethod = "every";
    }

}
