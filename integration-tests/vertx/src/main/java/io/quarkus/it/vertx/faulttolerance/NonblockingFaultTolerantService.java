package io.quarkus.it.vertx.faulttolerance;

import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class NonblockingFaultTolerantService {
    static final Queue<String> invocationThreads = new ConcurrentLinkedQueue<>();

    @NonBlocking
    @Retry(maxRetries = 10, delay = 5, delayUnit = ChronoUnit.MILLIS)
    @Fallback(fallbackMethod = "fallback")
    public Uni<String> hello() {
        invocationThreads.add(Thread.currentThread().getName());
        return Uni.createFrom().failure(new Exception());
    }

    public Uni<String> fallback() {
        invocationThreads.add(Thread.currentThread().getName());
        return Uni.createFrom().item("Hello fallback!");
    }
}
