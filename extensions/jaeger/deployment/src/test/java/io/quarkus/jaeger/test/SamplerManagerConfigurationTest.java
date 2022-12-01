package io.quarkus.jaeger.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.jaegertracing.Configuration.SamplerConfiguration;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests verifying configuration of a remote sampler manager.
 *
 */
public class SamplerManagerConfigurationTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withEmptyApplication()
            .overrideConfigKey("quarkus.jaeger.service-name", "my-service")
            .overrideConfigKey("quarkus.jaeger.sampler-manager-host-port", "my-jaeger-host:5778");

    /**
     * Verifies that the {@code JAEGER_SAMPLER_MANAGER_HOST_PORT} system property is set to the
     * host name and port specified in the {@code quarkus.jaeger.sampler-manager-host-port} property.
     */
    @Test
    void testSamplerManagerHostIsSetCorrectly() {
        SamplerConfiguration config = SamplerConfiguration.fromEnv();
        assertEquals("my-jaeger-host:5778", config.getManagerHostPort());
    }
}
