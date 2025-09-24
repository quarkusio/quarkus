package io.quarkus.micrometer.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.Util;
import io.quarkus.test.QuarkusUnitTest;

public class MicrometerBinderEnableAllPrecedenceOverBinderEnabledDefaultTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder.enable-all", "true")
            .overrideConfigKey("quarkus.micrometer.binder.jvm", "false")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withApplicationRoot((jar) -> jar
                    .addClasses(Util.class));

    @Inject
    MeterRegistry registry;

    @Test
    @Disabled("Should have any registered MeterRegistry objects when binder-enabled-default is disabled and enabled-all is enabled")
    public void testMeterRegistryPresent() {
        Assertions.assertNotNull(registry, "A registry should be configured");

        Assertions.assertNotNull(registry.find("jvm.info").counter(),
                "JVM Info counter should be present, found: " + Util.listMeters(registry, "jvm.info"));
    }
}
