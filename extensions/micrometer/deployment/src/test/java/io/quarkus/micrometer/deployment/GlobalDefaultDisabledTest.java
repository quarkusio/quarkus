package io.quarkus.micrometer.deployment;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Should not have any registered MeterRegistry objects when micrometer is disabled
 */
public class GlobalDefaultDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .withApplicationRoot((jar) -> jar
                    .addClasses(Util.class));

    @Inject
    MeterRegistry registry;

    @Test
    public void testMeterRegistryPresent() {
        // Composite Meter Registry
        Assertions.assertNotNull(registry, "A registry should be configured");

        Assertions.assertTrue(registry instanceof CompositeMeterRegistry,
                "Injected registry should be a CompositeMeterRegistry, was " + registry.getClass().getName());

        Assertions.assertTrue(((CompositeMeterRegistry) registry).getRegistries().isEmpty(),
                "No child registries should be present: " + ((CompositeMeterRegistry) registry).getRegistries());

        Assertions.assertNull(registry.find("jvm.info").counter(),
                "JVM Info counter should not be present, found: " + Util.listMeters(registry, "jvm.info"));
    }
}
