package io.quarkus.smallrye.faulttolerance.test.retry;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class RetryBean {
    private AtomicBoolean retries = new AtomicBoolean();

    @Retry
    public boolean retry() {
        if (!retries.get()) {
            retries.set(true);
            throw new RuntimeException("should retry");
        }
        return retries.get();
    }
}
