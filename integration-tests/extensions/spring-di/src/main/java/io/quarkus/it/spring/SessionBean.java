package io.quarkus.it.spring;

import java.util.concurrent.atomic.AtomicInteger;

@SessionService
public class SessionBean {

    final AtomicInteger value;

    public SessionBean() {
        this.value = new AtomicInteger();
    }

    public int getValue() {
        return value.getAndIncrement();
    }
}
