package io.quarkus.smallrye.faulttolerance.test.asynchronous.types.mutiny.resubscription;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MutinyHelloService {
    static final AtomicInteger COUNTER = new AtomicInteger(0);

    @NonBlocking
    @Retry
    public Uni<String> hello() {
        COUNTER.incrementAndGet();
        return Uni.createFrom().failure(IllegalArgumentException::new);
    }
}
