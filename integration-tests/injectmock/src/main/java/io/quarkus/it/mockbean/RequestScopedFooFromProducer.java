package io.quarkus.it.mockbean;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;

public class RequestScopedFooFromProducer {

    static final AtomicBoolean CONSTRUCTED = new AtomicBoolean();

    public String ping() {
        return "bar";
    }

    @PostConstruct
    void init() {
        CONSTRUCTED.set(true);
    }

}
