package io.quarkus.it.opentelemetry.scheduler;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduler;

@ApplicationScoped
@Startup
public class JobDefinitionCounter {

    @Inject
    Scheduler scheduler;

    AtomicInteger counter;

    @PostConstruct
    void init() {
        counter = new AtomicInteger();
        scheduler.newJob("myJobDefinition").setCron("*/1 * * * * ?").setTask(ex -> {
            try {
                Thread.sleep(100l);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            counter.incrementAndGet();
        }).schedule();
    }

    public int get() {
        return counter.get();
    }
}
