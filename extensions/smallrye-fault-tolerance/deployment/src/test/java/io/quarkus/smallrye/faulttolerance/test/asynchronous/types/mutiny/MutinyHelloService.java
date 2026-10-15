package io.quarkus.smallrye.faulttolerance.test.asynchronous.types.mutiny;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MutinyHelloService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @Asynchronous
    @Retry(jitter = 50)
    @Fallback(fallbackMethod = "fallback")
    public Uni<String> helloAsynchronous() {
        COUNTER.incrementAndGet();
        return Uni.createFrom().failure(IllegalArgumentException::new);
    }

    @AsynchronousNonBlocking
    @Retry(jitter = 50)
    @Fallback(fallbackMethod = "fallback")
    public Uni<String> helloAsynchronousNonBlocking() {
        COUNTER.incrementAndGet();
        return Uni.createFrom().failure(IllegalArgumentException::new);
    }

    public Uni<String> fallback() {
        return Uni.createFrom().item(() -> "hello");
    }
}
