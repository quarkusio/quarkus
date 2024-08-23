package io.quarkus.arc.runtime;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import io.quarkus.runtime.LaunchMode;

@Dependent
public class LaunchModeProducer {

    @Produces
    LaunchMode mode() {
        return LaunchMode.current();
    }
}
