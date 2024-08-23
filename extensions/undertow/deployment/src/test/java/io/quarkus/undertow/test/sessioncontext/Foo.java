package io.quarkus.undertow.test.sessioncontext;

import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;

@SessionScoped
class Foo {

    AtomicLong counter;

    @PostConstruct
    void init() {
        counter = new AtomicLong();
    }

    long incrementAndGet() {
        return counter.incrementAndGet();
    }

}
