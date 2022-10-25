package io.quarkus.it.micrometer.prometheus;

import java.util.Arrays;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
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

    /** Enable histogram buckets for a specific timer */
    @Produces
    @Singleton
    public MeterFilter enableHistogram() {
        return new MeterFilter() {
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().equals("prime.number.test")) {
                    return DistributionStatisticConfig.builder()
                            .percentiles(0.5, 0.95) // median and 95th percentile
                            .percentilesHistogram(true) // histogram buckets (for use with prometheus histogram_quantile)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}
