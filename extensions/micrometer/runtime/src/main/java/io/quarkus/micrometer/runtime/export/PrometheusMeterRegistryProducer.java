package io.quarkus.micrometer.runtime.export;

import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.tracer.common.SpanContext;

/**
 * This producer is only registered if the {@code quarkus.micrometer.export.prometheus.default-registry} is set to {@code true}.
 */
public class PrometheusMeterRegistryProducer {

    @Produces
    @Singleton
    @Alternative
    @Priority(Interceptor.Priority.APPLICATION + 100)
    public PrometheusMeterRegistry registry(PrometheusConfig config, PrometheusRegistry collectorRegistry,
            Optional<SpanContext> exemplarSampler, Clock clock) {
        return new PrometheusMeterRegistry(config, collectorRegistry, clock, exemplarSampler.orElse(null));
    }

}
