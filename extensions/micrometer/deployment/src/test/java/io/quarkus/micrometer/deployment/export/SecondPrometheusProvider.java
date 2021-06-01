package io.quarkus.micrometer.deployment.export;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;

@Singleton
public class SecondPrometheusProvider {
    @Produces
    @Singleton
    public PrometheusMeterRegistry registry(CollectorRegistry collectorRegistry, Clock clock) {
        PrometheusMeterRegistry customRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, collectorRegistry,
                clock);
        customRegistry.config().commonTags("customKey", "customValue");
        return customRegistry;
    }
}
