package io.quarkus.micrometer.deployment.export;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.quarkus.arc.AlternativePriority;

@Singleton
public class PreferredPrometheusProvider {
    @Produces
    @Singleton
    @AlternativePriority(Interceptor.Priority.APPLICATION + 1)
    public PrometheusMeterRegistry registry(CollectorRegistry collectorRegistry, Clock clock) {
        return new MyPrometheusMeterRegistry(PrometheusConfig.DEFAULT, collectorRegistry, clock);
    }

    static class MyPrometheusMeterRegistry extends PrometheusMeterRegistry {

        public MyPrometheusMeterRegistry(PrometheusConfig config, CollectorRegistry registry, Clock clock) {
            super(config, registry, clock);
        }
    }
}
