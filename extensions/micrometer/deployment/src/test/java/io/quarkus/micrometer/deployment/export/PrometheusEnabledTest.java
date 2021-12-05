package io.quarkus.micrometer.deployment.export;

import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class PrometheusEnabledTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .withEmptyApplication();

    @Inject
    MeterRegistry registry;

    @Inject
    PrometheusMeterRegistry promRegistry;

    @Test
    public void testMeterRegistryPresent() {
        // Prometheus is enabled (only registry)
        Assertions.assertNotNull(registry, "A registry should be configured");
        Set<MeterRegistry> subRegistries = ((CompositeMeterRegistry) registry).getRegistries();
        Assertions.assertEquals(1, subRegistries.size(),
                "There should be only one configured subregistry. Found " + subRegistries);

        PrometheusMeterRegistry subPromRegistry = (PrometheusMeterRegistry) subRegistries.iterator().next();
        Assertions.assertEquals(PrometheusMeterRegistry.class, subPromRegistry.getClass(),
                "Should be PrometheusMeterRegistry");
        Assertions.assertEquals(subPromRegistry, promRegistry,
                "Should be the same bean as the injected PrometheusMeterRegistry");
    }

    @Test
    public void metricsEndpoint() {
        // RestAssured prepends /app for us
        RestAssured.given()
                .accept("application/json")
                .get("/q/metrics")
                .then()
                .log().all()
                .statusCode(406);

        RestAssured.given()
                .get("/q/metrics")
                .then()
                .statusCode(200);
    }
}
