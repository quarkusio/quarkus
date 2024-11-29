package io.quarkus.smallrye.faulttolerance.test.config;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

@ApplicationScoped
public class BulkheadConfigBean {
    @Bulkhead(value = 5)
    public void value(CompletableFuture<?> barrier) {
        barrier.join();
    }

    @Bulkhead(value = 1, waitingTaskQueue = 5)
    @Asynchronous
    public Future<Void> waitingTaskQueue(CompletableFuture<?> barrier) {
        barrier.join();
        return completedFuture(null);
    }
}
