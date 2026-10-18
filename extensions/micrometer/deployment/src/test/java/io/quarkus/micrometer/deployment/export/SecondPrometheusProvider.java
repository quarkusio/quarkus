package io.quarkus.micrometer.deployment.export;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

@Singleton
public class SecondPrometheusProvider {
    @Produces
    @Singleton
    public PrometheusMeterRegistry registry(PrometheusRegistry collectorRegistry, Clock clock) {
        PrometheusMeterRegistry customRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, collectorRegistry,
                clock);
        customRegistry.config().commonTags("customKey", "customValue");
        return customRegistry;
    }
}
