package io.quarkus.smallrye.faulttolerance.test.retry.when;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.RetryWhen;

@ApplicationScoped
public class RetryOnAndRetryWhenExceptionService {
    @Retry(retryOn = IllegalStateException.class)
    @RetryWhen(exception = IsIllegalArgumentException.class)
    public void hello() {
    }
}
