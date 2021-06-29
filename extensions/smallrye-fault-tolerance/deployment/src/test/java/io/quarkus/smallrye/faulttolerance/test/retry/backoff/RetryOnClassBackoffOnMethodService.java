package io.quarkus.smallrye.faulttolerance.test.retry.backoff;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ExponentialBackoff;

@ApplicationScoped
@Retry
public class RetryOnClassBackoffOnMethodService {
    @ExponentialBackoff
    public void hello() {
        throw new IllegalArgumentException();
    }
}
