package io.quarkus.smallrye.faulttolerance.test.reuse;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;

@Singleton
public class MyGuard {
    static final int THRESHOLD = 5;
    static final int DELAY = 500;
    static final String NAME = "hello";

    @Produces
    @Identifier("my-guard")
    public static final Guard GUARD = Guard.create()
            .withCircuitBreaker().requestVolumeThreshold(THRESHOLD).delay(DELAY, ChronoUnit.MILLIS).name(NAME).done()
            .build();
}
