package io.quarkus.arc.runtime;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

import io.quarkus.runtime.LaunchMode;

@Dependent
public class LaunchModeProducer {

    @Produces
    LaunchMode mode() {
        return LaunchMode.current();
    }
}
