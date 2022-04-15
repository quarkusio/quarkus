package io.quarkus.it.faulttolerance;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.ApplyFaultTolerance;

@ApplicationScoped
public class Service {

    static final int THRESHOLD = 2;

    private String name;

    @PostConstruct
    void init() {
        name = "Lucie";
    }

    @ApplyFaultTolerance("my-fault-tolerance")
    public String getName(AtomicInteger counter) {
        if (counter.incrementAndGet() >= THRESHOLD) {
            return name;
        }
        throw new IllegalStateException("Counter=" + counter.get());
    }

}
