package io.quarkus.undertow.test.builtinbeans;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;

@SessionScoped
class Counter {

    AtomicInteger counter;

    @PostConstruct
    void init() {
        counter = new AtomicInteger();
    }

    int incrementAndGet() {
        return counter.incrementAndGet();
    }
}
