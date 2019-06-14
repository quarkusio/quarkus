package io.quarkus.it.faulttolerance;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class Service {

    static final int THRESHOLD = 2;

    private String name;

    @PostConstruct
    void init() {
        name = "Lucie";
    }

    @Retry(maxRetries = 10)
    public String getName(AtomicInteger counter) {
        if (counter.incrementAndGet() >= THRESHOLD) {
            return name;
        }
        throw new IllegalStateException("Counter=" + counter.get());
    }

}
