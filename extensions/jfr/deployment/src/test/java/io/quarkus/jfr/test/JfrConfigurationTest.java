package io.quarkus.jfr.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.jfr.runtime.config.JfrRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;

public class JfrConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.jfr.enabled", "false")
            .overrideConfigKey("quarkus.jfr.rest.enabled", "false");

    @Inject
    JfrRuntimeConfig runtimeConfig;

    @Test
    void config() {
        assertFalse(runtimeConfig.enabled());
        assertFalse(runtimeConfig.restEnabled());
    }
}
