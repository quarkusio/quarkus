package io.quarkus.it.opentelemetry.quartz;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.quartz.QuartzScheduler;
import io.quarkus.runtime.Startup;

@ApplicationScoped
@Startup
public class FailedJobDefinitionScheduler {

    @Inject
    QuartzScheduler scheduler;

    @PostConstruct
    void init() {
        scheduler.newJob("myFailedJobDefinition").setCron("*/1 * * * * ?").setTask(ex -> {
            try {
                Thread.sleep(100l);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            throw new RuntimeException("error occurred in myFailedJobDefinition.");
        }).schedule();
    }

}
