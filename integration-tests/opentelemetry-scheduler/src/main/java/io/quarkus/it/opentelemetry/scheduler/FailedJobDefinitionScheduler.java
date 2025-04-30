package io.quarkus.it.opentelemetry.scheduler;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduler;

@ApplicationScoped
@Startup
public class FailedJobDefinitionScheduler {

    @Inject
    Scheduler scheduler;

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
