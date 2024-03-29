package io.quarkus.virtual.disabled;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class Counter {

    private final AtomicInteger counter = new AtomicInteger();

    public int increment() {
        return counter.incrementAndGet();
    }

}
