package io.quarkus.micrometer.deployment.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class PrometheusEnabledOnManagementInterfaceTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setFlatClassPath(true)
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.management.enabled", "true")
            .withEmptyApplication();

    @Test
    public void metricsEndpoint() {
        RestAssured.given()
                .accept("application/json")
                .get("http://localhost:9001/q/metrics")
                .then()
                .log().all()
                .statusCode(406);

        RestAssured.given()
                .get("http://localhost:9001/q/metrics")
                .then()
                .statusCode(200);
    }
}
