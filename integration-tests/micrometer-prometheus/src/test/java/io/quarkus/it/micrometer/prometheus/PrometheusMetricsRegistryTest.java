package io.quarkus.it.micrometer.prometheus;

import static io.quarkus.test.micrometer.PrometheusMetricsAssert.assertMetrics;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.prometheus.client.exporter.common.TextFormat;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test functioning prometheus endpoint.
 * Use test execution order to ensure one http server request measurement
 * is present when the endpoint is scraped.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PrometheusMetricsRegistryTest {

    @Test
    @Order(1)
    void testRegistryInjection() {
        when().get("/message").then().statusCode(200)
                .body(containsString("io.micrometer.core.instrument.composite.CompositeMeterRegistry"));
    }

    @Test
    @Order(2)
    void testUnknownUrl() {
        when().get("/message/notfound").then().statusCode(404);
    }

    @Test
    @Order(3)
    void testServerError() {
        when().get("/message/fail").then().statusCode(500);
    }

    @Test
    @Order(4)
    void testPathParameter() {
        given().header("foo", "bar").when().get("/message/item/123").then().statusCode(200);
    }

    @Test
    @Order(5)
    void testMultipleParameters() {
        when().get("/message/match/123/1").then().statusCode(200);

        when().get("/message/match/1/123").then().statusCode(200);

        when().get("/message/match/baloney").then().statusCode(200);
    }

    @Test
    @Order(6)
    void testPanacheCalls() {
        when().get("/fruit/create").then().statusCode(204);

        when().get("/fruit/all").then().statusCode(204);
    }

    @Test
    @Order(7)
    void testPrimeEndpointCalls() {
        when().get("/prime/7").then().statusCode(200)
                .body(containsString("is prime"));
    }

    @Test
    @Order(8)
    void testAllTheThings() {
        when().get("/all-the-things").then().statusCode(200)
                .body(containsString("OK"));
    }

    @Test
    @Order(9)
    void testTemplatedPathOnClass() {
        when().get("/template/path/anything").then().statusCode(200)
                .body(containsString("Received: anything"));
    }

    @Test
    @Order(10)
    void testSecuredEndpoint() {
        when().get("/secured/item/123").then().statusCode(401);
        given().auth().preemptive().basic("foo", "bar").when().get("/secured/item/321").then().statusCode(401);
        given().auth().preemptive().basic("scott", "reader").when().get("/secured/item/123").then().statusCode(200);
        given().auth().preemptive().basic("stuart", "writer").when().get("/secured/item/321").then().statusCode(200);
    }

    @Test
    @Order(11)
    void testTemplatedPathOnSubResource() {
        when().get("/root/r1/sub/s2").then().statusCode(200)
                .body(containsString("r1:s2"));
    }

    @Test
    @Order(20)
    void testPrometheusScrapeEndpointTextPlain() {
        assertMetrics(RestAssured.given().header("Accept", TextFormat.CONTENT_TYPE_004)
                .when().get("/q/metrics")
                .then().statusCode(200)
                .extract().asInputStream())

                // Prometheus body has ALL THE THINGS in no particular order

                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("registry", "prometheus"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("env", "test"))
                .hasMetric("http_server_requests_seconds_count")

                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("status", "404"), entry("uri", "NOT_FOUND"), entry("outcome", "CLIENT_ERROR"))

                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("status", "500"), entry("uri", "/message/fail"), entry("outcome", "SERVER_ERROR"))

                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("status", "200"), entry("uri", "/message"), entry("outcome", "SUCCESS"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/message/item/{id}"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("status", "200"), entry("uri", "/message/item/{id}"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/secured/item/{id}"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("status", "200"), entry("uri", "/secured/item/{id}"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("status", "401"), entry("uri", "/secured/item/{id}"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("dummy", "value"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("foo", "bar"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("foo_response", "value"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/message/match/{id}/{sub}"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/message/match/{other}"))

                .hasMetricWithExactLabels("http_server_requests_seconds_count",
                        entry("dummy", "val-anything"), entry("env", "test"), entry("env2", "test"),
                        entry("foo", "UNSET"), entry("foo_response", "UNSET"), entry("method", "GET"),
                        entry("outcome", "SUCCESS"), entry("registry", "prometheus"),
                        entry("status", "200"), entry("uri", "/template/path/{value}"))

                .hasMetricWithExactLabels("http_server_requests_seconds_count",
                        entry("dummy", "value"), entry("env", "test"), entry("env2", "test"),
                        entry("foo", "UNSET"), entry("foo_response", "UNSET"), entry("method", "GET"),
                        entry("outcome", "SUCCESS"), entry("registry", "prometheus"),
                        entry("status", "200"), entry("uri", "/root/{rootParam}/sub/{subParam}"))

                // Verify Hibernate Metrics
                .hasMetricWithExactLabelsAndValue("hibernate_sessions_open_total", 2.0,
                        entry("entityManagerFactory", "<default>"), entry("env", "test"),
                        entry("env2", "test"), entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("hibernate_sessions_closed_total", 2.0,
                        entry("entityManagerFactory", "<default>"), entry("env", "test"),
                        entry("env2", "test"), entry("registry", "prometheus"))
                .hasMetricWithExactLabels("hibernate_connections_obtained_total",
                        entry("entityManagerFactory", "<default>"), entry("env", "test"),
                        entry("env2", "test"), entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("hibernate_entities_inserts_total", 3.0,
                        entry("entityManagerFactory", "<default>"), entry("env", "test"),
                        entry("env2", "test"), entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("hibernate_flushes_total", 1.0,
                        entry("entityManagerFactory", "<default>"), entry("env", "test"),
                        entry("env2", "test"), entry("registry", "prometheus"))

                // Annotated counters
                .doesNotHaveMetric("metric_none")
                .hasMetricWithExactLabelsAndValue("metric_all_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("exception", "none"),
                        entry("extra", "tag"), entry("fail", "false"),
                        entry("method", "countAllInvocations"), entry("registry", "prometheus"),
                        entry("result", "success"))
                .hasMetricWithExactLabelsAndValue("metric_all_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("extra", "tag"),
                        entry("fail", "true"), entry("method", "countAllInvocations"),
                        entry("registry", "prometheus"), entry("result", "failure"))
                .hasMetricWithExactLabelsAndValue("method_counted_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("fail", "prefix true"),
                        entry("method", "emptyMetricName"), entry("registry", "prometheus"),
                        entry("result", "failure"))
                .hasMetricWithExactLabelsAndValue("method_counted_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("exception", "none"),
                        entry("fail", "prefix false"), entry("method", "emptyMetricName"),
                        entry("registry", "prometheus"), entry("result", "success"))
                .doesNotHaveMetric("async_none")
                .hasMetricWithExactLabelsAndValue("async_all_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("do_fail_call", "true"), entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("extra", "tag"),
                        entry("method", "countAllAsyncInvocations"), entry("registry", "prometheus"),
                        entry("result", "failure"))
                .hasMetricWithExactLabelsAndValue("async_all_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("do_fail_call", "false"), entry("env", "test"), entry("env2", "test"),
                        entry("exception", "none"), entry("extra", "tag"),
                        entry("method", "countAllAsyncInvocations"), entry("registry", "prometheus"),
                        entry("result", "success"))
                .hasMetricWithExactLabelsAndValue("method_counted_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("fail", "42"),
                        entry("method", "emptyAsyncMetricName"), entry("registry", "prometheus"),
                        entry("result", "failure"))
                .hasMetricWithExactLabelsAndValue("method_counted_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("exception", "none"),
                        entry("fail", "42"), entry("method", "emptyAsyncMetricName"),
                        entry("registry", "prometheus"), entry("result", "success"))

                // Annotated Timers
                .hasMetricWithExactLabelsAndValue("call_seconds_count", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("extra", "tag"),
                        entry("method", "call"), entry("registry", "prometheus"))
                .hasMetricWithExactLabels("call_seconds_count",
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("exception", "none"),
                        entry("extra", "tag"), entry("method", "call"),
                        entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("async_call_seconds_count", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("extra", "tag"),
                        entry("method", "asyncCall"), entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("async_call_seconds_count", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("exception", "none"),
                        entry("extra", "tag"), entry("method", "asyncCall"),
                        entry("registry", "prometheus"))
                .hasMetricWithExactLabels("longCall_seconds_active_count",
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("extra", "tag"),
                        entry("method", "longCall"), entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("async_longCall_seconds_duration_sum", 0.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("extra", "tag"),
                        entry("method", "longAsyncCall"), entry("registry", "prometheus"))

                // Configured median, 95th percentile and histogram buckets
                .hasMetricWithExactLabels("prime_number_test_seconds",
                        entry("env", "test"), entry("env2", "test"),
                        entry("registry", "prometheus"), entry("quantile", "0.5"))
                .hasMetricWithExactLabels("prime_number_test_seconds",
                        entry("env", "test"), entry("env2", "test"),
                        entry("registry", "prometheus"), entry("quantile", "0.95"))
                .hasMetricWithExactLabels("prime_number_test_seconds_bucket",
                        entry("env", "test"), entry("env2", "test"),
                        entry("registry", "prometheus"), entry("le", "0.001"))

                // this was defined by a tag to a non-matching registry, and should not be found
                .doesNotHaveMetricWithLabels("http_server_requests_seconds_count",
                        entry("tag", "class-should-not-match"))

                // should not find this ignored uri
                .doesNotHaveMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/fruit/create"));
    }

    @Test
    @Order(20)
    void testPrometheusScrapeEndpointOpenMetrics() {
        assertMetrics(RestAssured.given().header("Accept", TextFormat.CONTENT_TYPE_OPENMETRICS_100)
                .when().get("/q/metrics")
                .then().statusCode(200)
                .extract().asInputStream())

                // Prometheus body has ALL THE THINGS in no particular order

                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("registry", "prometheus"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("env", "test"))
                .hasMetric("http_server_requests_seconds_count")

                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("status", "404"), entry("uri", "NOT_FOUND"), entry("outcome", "CLIENT_ERROR"))

                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("status", "500"), entry("uri", "/message/fail"), entry("outcome", "SERVER_ERROR"))

                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("status", "200"), entry("uri", "/message"), entry("outcome", "SUCCESS"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/message/item/{id}"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/message/match/{id}/{sub}"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/message/match/{other}"))

                .hasMetricWithExactLabels("http_server_requests_seconds_count",
                        entry("dummy", "val-anything"), entry("env", "test"), entry("env2", "test"),
                        entry("foo", "UNSET"), entry("foo_response", "UNSET"), entry("method", "GET"),
                        entry("outcome", "SUCCESS"), entry("registry", "prometheus"),
                        entry("status", "200"), entry("uri", "/template/path/{value}"))

                // Verify Hibernate Metrics
                .hasMetricWithExactLabelsAndValue("hibernate_sessions_open_total", 2.0,
                        entry("entityManagerFactory", "<default>"), entry("env", "test"),
                        entry("env2", "test"), entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("hibernate_sessions_closed_total", 2.0,
                        entry("entityManagerFactory", "<default>"), entry("env", "test"),
                        entry("env2", "test"), entry("registry", "prometheus"))
                .hasMetricWithExactLabels("hibernate_connections_obtained_total",
                        entry("entityManagerFactory", "<default>"), entry("env", "test"),
                        entry("env2", "test"), entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("hibernate_entities_inserts_total", 3.0,
                        entry("entityManagerFactory", "<default>"), entry("env", "test"),
                        entry("env2", "test"), entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("hibernate_flushes_total", 1.0,
                        entry("entityManagerFactory", "<default>"), entry("env", "test"),
                        entry("env2", "test"), entry("registry", "prometheus"))

                // Annotated counters
                .doesNotHaveMetric("metric_none")
                .hasMetricWithExactLabelsAndValue("metric_all_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("exception", "none"),
                        entry("extra", "tag"), entry("fail", "false"),
                        entry("method", "countAllInvocations"), entry("registry", "prometheus"),
                        entry("result", "success"))
                .hasMetricWithExactLabelsAndValue("metric_all_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("extra", "tag"),
                        entry("fail", "true"), entry("method", "countAllInvocations"),
                        entry("registry", "prometheus"), entry("result", "failure"))
                .hasMetricWithExactLabelsAndValue("method_counted_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("fail", "prefix true"),
                        entry("method", "emptyMetricName"), entry("registry", "prometheus"),
                        entry("result", "failure"))
                .hasMetricWithExactLabelsAndValue("method_counted_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("exception", "none"),
                        entry("fail", "prefix false"), entry("method", "emptyMetricName"),
                        entry("registry", "prometheus"), entry("result", "success"))
                .doesNotHaveMetric("async_none")
                .hasMetricWithExactLabelsAndValue("async_all_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("do_fail_call", "true"), entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("extra", "tag"),
                        entry("method", "countAllAsyncInvocations"), entry("registry", "prometheus"),
                        entry("result", "failure"))
                .hasMetricWithExactLabelsAndValue("async_all_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("do_fail_call", "false"), entry("env", "test"), entry("env2", "test"),
                        entry("exception", "none"), entry("extra", "tag"),
                        entry("method", "countAllAsyncInvocations"), entry("registry", "prometheus"),
                        entry("result", "success"))
                .hasMetricWithExactLabelsAndValue("method_counted_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("fail", "42"),
                        entry("method", "emptyAsyncMetricName"), entry("registry", "prometheus"),
                        entry("result", "failure"))
                .hasMetricWithExactLabelsAndValue("method_counted_total", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("exception", "none"),
                        entry("fail", "42"), entry("method", "emptyAsyncMetricName"),
                        entry("registry", "prometheus"), entry("result", "success"))

                // Annotated Timers
                .hasMetricWithExactLabelsAndValue("call_seconds_count", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("extra", "tag"),
                        entry("method", "call"), entry("registry", "prometheus"))
                .hasMetricWithExactLabels("call_seconds_count",
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("exception", "none"),
                        entry("extra", "tag"), entry("method", "call"),
                        entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("async_call_seconds_count", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"),
                        entry("exception", "NullPointerException"), entry("extra", "tag"),
                        entry("method", "asyncCall"), entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("async_call_seconds_count", 1.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("exception", "none"),
                        entry("extra", "tag"), entry("method", "asyncCall"),
                        entry("registry", "prometheus"))
                .hasMetricWithExactLabels("longCall_seconds_active_count",
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("extra", "tag"),
                        entry("method", "longCall"), entry("registry", "prometheus"))
                .hasMetricWithExactLabelsAndValue("async_longCall_seconds_duration_sum", 0.0,
                        entry("class", "io.quarkus.it.micrometer.prometheus.AnnotatedResource"),
                        entry("env", "test"), entry("env2", "test"), entry("extra", "tag"),
                        entry("method", "longAsyncCall"), entry("registry", "prometheus"))

                // Configured median, 95th percentile and histogram buckets
                .hasMetricWithExactLabels("prime_number_test_seconds",
                        entry("env", "test"), entry("env2", "test"),
                        entry("registry", "prometheus"), entry("quantile", "0.5"))
                .hasMetricWithExactLabels("prime_number_test_seconds",
                        entry("env", "test"), entry("env2", "test"),
                        entry("registry", "prometheus"), entry("quantile", "0.95"))
                .hasMetricWithExactLabels("prime_number_test_seconds_bucket",
                        entry("env", "test"), entry("env2", "test"),
                        entry("registry", "prometheus"), entry("le", "0.001"))

                // this was defined by a tag to a non-matching registry, and should not be found
                .doesNotHaveMetricWithLabels("http_server_requests_seconds_count",
                        entry("tag", "class-should-not-match"))

                // should not find this ignored uri
                .doesNotHaveMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/fruit/create"));
    }
}
