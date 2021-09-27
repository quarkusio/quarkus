package io.quarkus.micrometer.deployment.export;

import java.util.Set;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.quarkus.micrometer.runtime.registry.json.JsonMeterRegistry;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class JsonRegistryEnabledTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .overrideConfigKey("quarkus.http.non-application-root-path", "relative")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.export.json.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    MeterRegistry registry;

    @Inject
    JsonMeterRegistry jsonMeterRegistry;

    @Test
    public void testMeterRegistryPresent() {
        // Prometheus is enabled (only registry)
        Assertions.assertNotNull(registry, "A registry should be configured");
        Set<MeterRegistry> subRegistries = ((CompositeMeterRegistry) registry).getRegistries();
        JsonMeterRegistry subPromRegistry = (JsonMeterRegistry) subRegistries.iterator().next();
        Assertions.assertEquals(JsonMeterRegistry.class, subPromRegistry.getClass(), "Should be JsonMeterRegistry");
        Assertions.assertEquals(subPromRegistry, jsonMeterRegistry,
                "The only MeterRegistry should be the same bean as the JsonMeterRegistry");
    }

    @Test
    public void metricsEndpoint() {
        // RestAssured prepends /app for us
        RestAssured.given()
                .accept("application/json")
                .get("/relative/metrics")
                .then()
                .statusCode(200);
    }
}
