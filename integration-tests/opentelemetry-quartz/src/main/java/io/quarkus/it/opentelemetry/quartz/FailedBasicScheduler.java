package io.quarkus.it.opentelemetry.quartz;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class FailedBasicScheduler {

    @Scheduled(cron = "*/1 * * * * ?", identity = "myFailedBasicScheduler")
    void init() throws InterruptedException {
        Thread.sleep(100l);
        throw new RuntimeException("error occurred in myFailedBasicScheduler.");

    }

}
