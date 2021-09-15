package io.quarkus.micrometer.runtime.export;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.quarkus.arc.AlternativePriority;

/**
 * This producer is only registered if the {@code quarkus.micrometer.export.prometheus.default-registry} is set to {@code true}.
 */
public class PrometheusMeterRegistryProducer {

    @Produces
    @Singleton
    @AlternativePriority(Interceptor.Priority.APPLICATION + 100)
    public PrometheusMeterRegistry registry(PrometheusConfig config, CollectorRegistry collectorRegistry, Clock clock) {
        return new PrometheusMeterRegistry(config, collectorRegistry, clock);
    }

}
