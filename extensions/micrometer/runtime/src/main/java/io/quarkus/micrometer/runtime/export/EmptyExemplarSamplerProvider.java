package io.quarkus.micrometer.runtime.export;

import java.util.Optional;

import jakarta.enterprise.inject.Produces;

import io.prometheus.client.exemplars.ExemplarSampler;

public class EmptyExemplarSamplerProvider {

    @Produces
    public Optional<ExemplarSampler> exemplarSampler() {
        return Optional.empty();
    }
}
