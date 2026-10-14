package io.quarkus.it.mockbean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.smallrye.config.Config;

public class DummyMappingProducer {

    @Inject
    Config config;

    @Produces
    @ApplicationScoped
    @io.quarkus.test.Mock
    DummyMapping server() {
        return config.getConfigMapping(DummyMapping.class);
    }
}
