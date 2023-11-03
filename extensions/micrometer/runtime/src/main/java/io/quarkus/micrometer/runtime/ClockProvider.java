package io.quarkus.micrometer.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Clock;
import io.quarkus.arc.DefaultBean;

@Singleton
public class ClockProvider {
    @Produces
    @Singleton
    @DefaultBean
    public Clock clock() {
        return Clock.SYSTEM;
    }
}
