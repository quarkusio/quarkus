package io.quarkus.micrometer.deployment.export;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
            .overrideConfigKey("quarkus.micrometer.export.azuremonitor.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.export.datadog.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.export.jmx.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.export.signalfx.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.export.stackdriver.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.export.statsd.enabled", "false")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

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
        RestAssured.when().get("/prometheus").then().statusCode(404);
    }
}
