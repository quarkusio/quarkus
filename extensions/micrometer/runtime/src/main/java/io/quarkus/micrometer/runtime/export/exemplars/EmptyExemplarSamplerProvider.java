package io.quarkus.micrometer.runtime.export.exemplars;

import java.util.Optional;

import jakarta.enterprise.inject.Produces;

import io.prometheus.metrics.tracer.common.SpanContext;

public class EmptyExemplarSamplerProvider {

    @Produces
    public Optional<SpanContext> exemplarSampler() {
        return Optional.empty();
    }
}
