package io.quarkus.smallrye.faulttolerance.test.retry.backoff;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.FibonacciBackoff;

@ApplicationScoped
@FibonacciBackoff
public class RetryOnMethodBackoffOnClassService {
    @Retry
    public void hello() {
        throw new IllegalArgumentException();
    }
}
