package io.quarkus.smallrye.faulttolerance.test.reuse.config;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.smallrye.common.annotation.Identifier;
import io.smallrye.faulttolerance.api.Guard;

@Singleton
public class MyGuard {
    static final String NAME = "hello";

    @Produces
    @Identifier("my-guard")
    public static final Guard GUARD = Guard.create()
            .withCircuitBreaker().requestVolumeThreshold(10).delay(5000, ChronoUnit.MILLIS).name(NAME).done()
            .build();
}
