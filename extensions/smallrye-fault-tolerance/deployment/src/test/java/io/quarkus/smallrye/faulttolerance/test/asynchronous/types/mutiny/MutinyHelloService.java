package io.quarkus.smallrye.faulttolerance.test.asynchronous.types.mutiny;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MutinyHelloService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @NonBlocking
    @Retry(jitter = 50)
    @Fallback(fallbackMethod = "fallback")
    public Uni<String> helloNonblocking() {
        COUNTER.incrementAndGet();
        return Uni.createFrom().failure(IllegalArgumentException::new);
    }

    @Blocking
    @Retry(jitter = 50)
    @Fallback(fallbackMethod = "fallback")
    public Uni<String> helloBlocking() {
        COUNTER.incrementAndGet();
        return Uni.createFrom().failure(IllegalArgumentException::new);
    }

    @Asynchronous
    @NonBlocking
    @Retry(jitter = 50)
    @Fallback(fallbackMethod = "fallback")
    public Uni<String> helloAsynchronousNonblocking() {
        COUNTER.incrementAndGet();
        return Uni.createFrom().failure(IllegalArgumentException::new);
    }

    @Asynchronous
    @Retry(jitter = 50)
    @Fallback(fallbackMethod = "fallback")
    public Uni<String> helloAsynchronous() {
        COUNTER.incrementAndGet();
        return Uni.createFrom().failure(IllegalArgumentException::new);
    }

    @Asynchronous
    @Blocking
    @Retry(jitter = 50)
    @Fallback(fallbackMethod = "fallback")
    public Uni<String> helloAsynchronousBlocking() {
        COUNTER.incrementAndGet();
        return Uni.createFrom().failure(IllegalArgumentException::new);
    }

    public Uni<String> fallback() {
        return Uni.createFrom().item(() -> "hello");
    }
}
