package io.quarkus.micrometer.deployment.export;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.micrometer.runtime.export.handlers.PrometheusHandler;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

/**
 * The JSON endpoint is registered under the Prometheus path, which also answers every sub-path. Each combination
 * must return the media type it advertises, with a body actually in that format.
 */
public class JsonOnSubPathOfPrometheusTest {

    private static final String OPENMETRICS = PrometheusHandler.CONTENT_TYPE_OPENMETRICS_100;

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
        assertJson(RestAssured.given().get("/q/metrics/json").then().statusCode(200)
                .contentType(startsWith("application/json"))
                .extract().response());
    }

    @Test
    public void jsonPathServesJsonWithAcceptHeader() {
        assertJson(RestAssured.given().accept("application/json").get("/q/metrics/json").then().statusCode(200)
                .contentType(startsWith("application/json"))
                .extract().response());
    }

    @Test
    public void prometheusPathServesOpenMetricsWithoutAcceptHeader() {
        Response response = RestAssured.given().get("/q/metrics").then().statusCode(200)
                .contentType(OPENMETRICS)
                .extract().response();
        assertOpenMetrics(response);
    }

    @Test
    public void prometheusPathServesPlainTextWhenAsked() {
        assertPrometheusText(RestAssured.given().accept("text/plain").get("/q/metrics").then().statusCode(200)
                .contentType(startsWith("text/plain"))
                .extract().response());
    }

    @Test
    public void textAcceptHeaderUnderTheJsonPathFallsThroughToPrometheus() {
        assertPrometheusText(RestAssured.given().accept("text/plain").get("/q/metrics/json").then().statusCode(200)
                .contentType(startsWith("text/plain"))
                .extract().response());
    }

    @Test
    public void openMetricsAcceptHeaderUnderTheJsonPathFallsThroughToPrometheus() {
        assertOpenMetrics(RestAssured.given().accept(OPENMETRICS).get("/q/metrics/json").then().statusCode(200)
                .contentType(OPENMETRICS)
                .extract().response());
    }

    @Test
    public void prometheusPathDoesNotServeJson() {
        RestAssured.given()
                .accept("application/json")
                .get("/q/metrics")
                .then()
                .statusCode(406);
    }

    /**
     * The body must parse as JSON and expose the meters as members, rather than merely contain a quoted name.
     */
    private static void assertJson(Response response) {
        String body = response.asString();
        assertThat(body, body.trim(), both(startsWith("{")).and(endsWith("}")));
        JsonPath json = new JsonPath(body);
        Map<String, ?> meters = json.getMap("$");
        Assertions.assertNotNull(meters, body);
        String meter = meters.keySet().stream().filter(k -> k.startsWith("jvm.info")).findFirst()
                .orElseThrow(() -> new AssertionError("No jvm.info meter among " + meters.keySet()));
        assertThat(body, json.get("'" + meter + "'"), notNullValue());
        Assertions.assertFalse(body.contains("# HELP"), body);
    }

    /**
     * The Prometheus text format names a counter by its {@code _total} sample name in the metadata lines, and has
     * no end-of-stream marker.
     */
    private static void assertPrometheusText(Response response) {
        String body = response.asString();
        assertThat(body, body, containsString("# HELP jvm_total"));
        assertThat(body, body, containsString("# TYPE jvm_total counter"));
        Assertions.assertFalse(body.contains("# EOF"), body);
        assertMetricSamples(body);
    }

    /**
     * OpenMetrics drops the {@code _total} suffix from the metadata of a counter and terminates the body with an
     * {@code # EOF} line.
     */
    private static void assertOpenMetrics(Response response) {
        String body = response.asString();
        assertThat(body, body, containsString("# TYPE jvm counter"));
        assertThat(body, body.trim(), endsWith("# EOF"));
        assertMetricSamples(body);
    }

    private static void assertMetricSamples(String body) {
        assertThat(body, body, containsString("jvm_total{runtime=\""));
        assertThat(body, body.trim(), startsWith("#"));
        Assertions.assertFalse(body.trim().startsWith("{"), body);
    }
}
