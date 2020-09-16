package io.quarkus.it.micrometer.jmx;

import java.util.Arrays;

import javax.annotation.Priority;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.jmx.JmxMeterRegistry;
import io.quarkus.micrometer.runtime.MeterFilterConstraint;

@Singleton
@Priority(Interceptor.Priority.APPLICATION - 100)
public class CustomConfiguration {

    @Produces
    @Singleton
    @MeterFilterConstraint(applyTo = JmxMeterRegistry.class)
    public MeterFilter configurePrometheusRegistries() {
        return MeterFilter.commonTags(Arrays.asList(
                Tag.of("registry", "jmx")));
    }

    @Produces
    @Singleton
    @MeterFilterConstraint(applyTo = CustomConfiguration.class)
    public MeterFilter configureNonexistantRegistries() {
        return MeterFilter.commonTags(Arrays.asList(
                Tag.of("tag", "class-should-not-match")));
    }

    @Produces
    @Singleton
    public MeterFilter configureAllRegistries() {
        return MeterFilter.commonTags(Arrays.asList(
                Tag.of("env", "test")));
    }
}
