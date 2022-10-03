package io.quarkus.micrometer.runtime.export;

import java.util.Optional;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exemplars.ExemplarSampler;
import io.quarkus.arc.Priority;

/**
 * This producer is only registered if the {@code quarkus.micrometer.export.prometheus.default-registry} is set to {@code true}.
 */
public class PrometheusMeterRegistryProducer {

    @Produces
    @Singleton
    @Alternative
    @Priority(Interceptor.Priority.APPLICATION + 100)
    public PrometheusMeterRegistry registry(PrometheusConfig config, CollectorRegistry collectorRegistry,
            Optional<ExemplarSampler> exemplarSampler, Clock clock) {
        return new PrometheusMeterRegistry(config, collectorRegistry, clock, exemplarSampler.orElse(null));
    }

}
