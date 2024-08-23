package io.quarkus.micrometer.deployment;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Should not have any registered MeterRegistry objects when micrometer is disabled
 */
public class MicrometerDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("quarkus.micrometer.enabled=false"), "application.properties"))
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .assertException(t -> {
                Assertions.assertEquals(DeploymentException.class, t.getClass());
            });

    @Inject
    MeterRegistry registry;

    @Test
    public void testNoMeterRegistry() {
        //Should not be reached: dump what was injected if it somehow passed
        Assertions.assertNull(registry, "A MeterRegistry should not be found/injected when micrometer is disabled");
    }
}
