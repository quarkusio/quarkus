package io.quarkus.smallrye.faulttolerance.test.config;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

@Dependent
@Retry
public class ConfigPropertyBean {
    private int retry = 0;

    @Retry
    public void triggerException() {
        retry++;
        throw new IllegalStateException("Exception");
    }

    public int getRetry() {
        return retry;
    }
}
