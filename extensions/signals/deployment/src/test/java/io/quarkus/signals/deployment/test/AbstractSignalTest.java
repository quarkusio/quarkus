package io.quarkus.signals.deployment.test;

import java.time.Duration;

public abstract class AbstractSignalTest {

    protected Duration defaultTimeout() {
        return Duration.ofSeconds(5);
    }
}
