package io.quarkus.micrometer.runtime;

import static javax.interceptor.Interceptor.Priority.PLATFORM_AFTER;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.quarkus.arc.AlternativePriority;

/**
 * @return the single resolveable "root" MeterRegistry
 */
public class CompositeRegistryCreator {
    @Produces
    @Singleton
    @AlternativePriority(PLATFORM_AFTER)
    public MeterRegistry produceRootRegistry() {
        return Metrics.globalRegistry;
    }
}
