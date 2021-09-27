package io.quarkus.micrometer.deployment.export;

import static org.hamcrest.Matchers.containsString;

import java.util.Set;

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

public class JsonAndPrometheusRegistryEnabledTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.http.root-path", "/app")
            .overrideConfigKey("quarkus.http.non-application-root-path", "relative")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.jvm", "true")
            .overrideConfigKey("quarkus.micrometer.export.json.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    MeterRegistry registry;

    @Test
    public void testMeterRegistryPresent() {
        Assertions.assertNotNull(registry, "A registry should be configured");
        Set<MeterRegistry> subRegistries = ((CompositeMeterRegistry) registry).getRegistries();
        Assertions.assertEquals(2, subRegistries.size(), "Should have two sub-registries, found " + subRegistries);
    }

    @Test
    public void metricsEndpoint() {
        // RestAssured prepends /app for us
        RestAssured.given()
                .accept("application/json")
                .get("/relative/metrics")
                .then()
                .statusCode(200)
                .body(containsString("    \"jvm.info;runtime="));

        RestAssured.given()
                .get("/relative/metrics")
                .then()
                .statusCode(200)
                .body(containsString("jvm_info_total{runtime=\""));
    }
}
