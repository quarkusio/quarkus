package io.quarkus.micrometer.deployment.export;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Should not have any registered MeterRegistry objects when micrometer is disabled
 */
public class AllRegistriesDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.export.json.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "false")
            .withEmptyApplication();

    @Inject
    MeterRegistry registry;

    @Test
    public void testMeterRegistryPresent() {
        // Composite Meter Registry
        Assertions.assertNotNull(registry, "A registry should be configured");
        Assertions.assertEquals(CompositeMeterRegistry.class, registry.getClass(), "Should be CompositeMeterRegistry");
    }

    @Test
    public void testNoPrometheusEndpoint() {
        // Micrometer is enabled, prometheus is not.
        RestAssured.when().get("/q/metrics").then().statusCode(404);
    }
}
