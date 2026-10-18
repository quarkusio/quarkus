package io.quarkus.smallrye.faulttolerance.test.retry.when;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.RetryWhen;

@Dependent
@RetryWhen
public class RetryOnMethodRetryWhenOnClassService {
    @Retry
    public void hello() {
        throw new IllegalArgumentException();
    }
}
