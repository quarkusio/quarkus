package io.quarkus.it.spring.scheduled;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.springframework.scheduling.annotation.Scheduled;

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
