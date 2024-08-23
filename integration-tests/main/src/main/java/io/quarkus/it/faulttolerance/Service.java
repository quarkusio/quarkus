package io.quarkus.it.faulttolerance;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

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

    @Retry // set of `retryOn` exceptions is configured in application.properties
    public String retriedMethod(AtomicInteger counter) {
        if (counter.incrementAndGet() >= THRESHOLD) {
            return name;
        }
        throw new MyFaultToleranceError();
    }
}
