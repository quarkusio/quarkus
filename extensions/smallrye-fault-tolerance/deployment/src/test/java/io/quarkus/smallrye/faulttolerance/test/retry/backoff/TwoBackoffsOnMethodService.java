package io.quarkus.smallrye.faulttolerance.test.retry.backoff;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;

@ApplicationScoped
public class TwoBackoffsOnMethodService {
    @Retry
    @ExponentialBackoff
    @FibonacciBackoff
    public void hello() {
        throw new IllegalArgumentException();
    }
}
