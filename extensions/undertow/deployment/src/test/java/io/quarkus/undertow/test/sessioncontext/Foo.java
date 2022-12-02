package io.quarkus.undertow.test.sessioncontext;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;

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