package io.quarkus.smallrye.reactivemessaging.runtime;

import java.time.Duration;

public record WorkerPoolConfig(Integer maxConcurrency, Duration shutdownTimeout, Duration shutdownCheckInterval) {

    public WorkerPoolConfig(int maxConcurrency, int shutdownTimeoutMs, int shutdownCheckIntervalMs) {
        this(maxConcurrency, Duration.ofMillis(shutdownTimeoutMs), Duration.ofMillis(shutdownCheckIntervalMs));
    }

}
