package io.quarkus.it.micrometer.prometheus;

import java.util.Arrays;

import javax.annotation.Priority;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.quarkus.micrometer.runtime.MeterFilterConstraint;

@Singleton
@Priority(Interceptor.Priority.APPLICATION - 100)
public class CustomConfiguration {

    @ConfigProperty(name = "deployment.env")
    String deploymentEnv;

    @Produces
    @Singleton
    @MeterFilterConstraint(applyTo = PrometheusMeterRegistry.class)
    public MeterFilter configurePrometheusRegistries() {
        return MeterFilter.commonTags(Arrays.asList(
                Tag.of("registry", "prometheus")));
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
                Tag.of("env", deploymentEnv)));
    }

    /**
     * Produce a custom prometheus configuration that is isolated/separate from
     * the application (and won't be connected to the Quarkus configured application
     * endpoint).
     */
    @Produces
    @Singleton
    public PrometheusMeterRegistry registry(CollectorRegistry collectorRegistry, Clock clock) {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, collectorRegistry, clock);
    }
}
