package io.quarkus.micrometer.deployment.export;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * The JSON endpoint is registered under the Prometheus path, which also answers every sub-path.
 */
public class JsonOnSubPathOfPrometheusTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.otel.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.jvm", "true")
            .overrideConfigKey("quarkus.micrometer.export.json.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.export.json.path", "metrics/json")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withEmptyApplication();

    @Test
    public void jsonPathServesJsonWithoutAcceptHeader() {
        RestAssured.given()
                .get("/q/metrics/json")
                .then()
                .statusCode(200)
                .body(containsString("    \"jvm.info;runtime="));
    }

    @Test
    public void jsonPathServesJsonWithAcceptHeader() {
        RestAssured.given()
                .accept("application/json")
                .get("/q/metrics/json")
                .then()
                .statusCode(200)
                .body(containsString("    \"jvm.info;runtime="));
    }

    @Test
    public void prometheusPathStillServesPrometheusText() {
        RestAssured.given()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString("jvm_total{runtime=\""));

        RestAssured.given()
                .accept("text/plain")
                .get("/q/metrics/json")
                .then()
                .statusCode(200)
                .body(containsString("jvm_total{runtime=\""));
    }

    @Test
    public void prometheusPathDoesNotServeJson() {
        RestAssured.given()
                .accept("application/json")
                .get("/q/metrics")
                .then()
                .statusCode(406);
    }
}
