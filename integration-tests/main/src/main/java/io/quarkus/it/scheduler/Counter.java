package io.quarkus.it.scheduler;

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

    @Scheduled(cron = "0/1 * * * * ?")
    void increment() {
        counter.incrementAndGet();
    }

}
