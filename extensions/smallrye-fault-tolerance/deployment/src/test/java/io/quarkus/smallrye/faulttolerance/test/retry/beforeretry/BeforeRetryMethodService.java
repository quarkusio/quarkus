package io.quarkus.smallrye.faulttolerance.test.retry.beforeretry;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.BeforeRetry;

@Dependent
public class BeforeRetryMethodService {
    static final Set<Integer> ids = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger counter = new AtomicInteger();

    @Retry
    @BeforeRetry(methodName = "beforeRetry")
    public void hello() {
        throw new IllegalArgumentException();
    }

    void beforeRetry() {
        ids.add(counter.incrementAndGet());
    }
}
