package io.quarkus.it.spring;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;

@RequestService
public class RequestBean {
    static final AtomicInteger N = new AtomicInteger();
    int n;

    @PostConstruct
    public void postConstruct() {
        this.n = N.getAndIncrement();
    }

    public int getValue() {
        return n;
    }
}
