package io.quarkus.it.opentelemetry.quartz;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class Counter {

    AtomicInteger counter;

    @PostConstruct
    void init() {
        counter = new AtomicInteger();
    }

    public int get() {
        return counter.get();
    }

    @Scheduled(cron = "*/1 * * * * ?", identity = "myCounter")
    void increment() throws InterruptedException {
        Thread.sleep(100l);
        counter.incrementAndGet();
    }

}
