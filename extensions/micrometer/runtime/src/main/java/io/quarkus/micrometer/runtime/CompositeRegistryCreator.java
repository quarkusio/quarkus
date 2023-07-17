package io.quarkus.micrometer.runtime;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_AFTER;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

/**
 * @return the single resolvable "root" MeterRegistry
 */
public class CompositeRegistryCreator {
    @Produces
    @Singleton
    @Alternative
    @Priority(PLATFORM_AFTER)
    public MeterRegistry produceRootRegistry() {
        return Metrics.globalRegistry;
    }
}
