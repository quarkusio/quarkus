package io.quarkus.micrometer.deployment.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.prometheus.client.exporter.common.TextFormat;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class PrometheusFormatConfigPlainTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.format", "plain")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withEmptyApplication();

    @Test
    void testPrometheusFormatConfigPlainDefault() {
        RestAssured.given()
                .when().get("/q/metrics")
                .then().statusCode(200)
                .header("Content-Type", TextFormat.CONTENT_TYPE_004);
    }

    @Test
    void testPrometheusFormatConfigPlainWithOpenMetricsHeader() {
        RestAssured.given()
                .header("Accept", TextFormat.CONTENT_TYPE_OPENMETRICS_100)
                .when().get("/q/metrics")
                .then().statusCode(200)
                .header("Content-Type", TextFormat.CONTENT_TYPE_OPENMETRICS_100);
    }

    @Test
    void testPrometheusFormatConfigPlainWithTextPlainHeader() {
        RestAssured.given()
                .header("Accept", TextFormat.CONTENT_TYPE_004)
                .when().get("/q/metrics")
                .then().statusCode(200)
                .header("Content-Type", TextFormat.CONTENT_TYPE_004);
    }
}
