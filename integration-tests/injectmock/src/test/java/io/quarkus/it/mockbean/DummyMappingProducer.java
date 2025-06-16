package io.quarkus.it.mockbean;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;

import io.smallrye.config.SmallRyeConfig;

public class DummyMappingProducer {

    @Inject
    Config config;

    @Produces
    @ApplicationScoped
    @io.quarkus.test.Mock
    DummyMapping server() {
        return config.unwrap(SmallRyeConfig.class).getConfigMapping(DummyMapping.class);
    }
}
