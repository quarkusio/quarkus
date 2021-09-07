package io.quarkus.it.micrometer.prometheus;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;

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
        when().get("/messsage/notfound").then().statusCode(404);
    }

    @Test
    @Order(3)
    void testServerError() {
        when().get("/message/fail").then().statusCode(500);
    }

    @Test
    @Order(4)
    void testPathParameter() {
        when().get("/message/item/123").then().statusCode(200);
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
    void testPrometheusScrapeEndpoint() {
        when().get("/q/metrics").then().statusCode(200)

                // Prometheus body has ALL THE THINGS in no particular order

                .body(containsString("registry=\"prometheus\""))
                .body(containsString("env=\"test\""))
                .body(containsString("http_server_requests"))

                .body(containsString("status=\"404\""))
                .body(containsString("uri=\"NOT_FOUND\""))
                .body(containsString("outcome=\"CLIENT_ERROR\""))

                .body(containsString("status=\"500\""))
                .body(containsString("uri=\"/message/fail\""))
                .body(containsString("outcome=\"SERVER_ERROR\""))

                .body(containsString("status=\"200\""))
                .body(containsString("uri=\"/message\""))
                .body(containsString("uri=\"/message/item/{id}\""))
                .body(containsString("outcome=\"SUCCESS\""))
                .body(containsString("uri=\"/message/match/{id}/{sub}\""))
                .body(containsString("uri=\"/message/match/{other}\""))

                .body(containsString(
                        "http_server_requests_seconds_count{env=\"test\",method=\"GET\",outcome=\"SUCCESS\",registry=\"prometheus\",status=\"200\",uri=\"/template/path/{value}\""))

                // Verify Hibernate Metrics
                .body(containsString(
                        "hibernate_sessions_open_total{entityManagerFactory=\"<default>\",env=\"test\",registry=\"prometheus\",} 2.0"))
                .body(containsString(
                        "hibernate_sessions_closed_total{entityManagerFactory=\"<default>\",env=\"test\",registry=\"prometheus\",} 2.0"))
                .body(containsString(
                        "hibernate_connections_obtained_total{entityManagerFactory=\"<default>\",env=\"test\",registry=\"prometheus\",}"))
                .body(containsString(
                        "hibernate_entities_inserts_total{entityManagerFactory=\"<default>\",env=\"test\",registry=\"prometheus\",} 3.0"))
                .body(containsString(
                        "hibernate_flushes_total{entityManagerFactory=\"<default>\",env=\"test\",registry=\"prometheus\",} 1.0"))

                // Annotated counters
                .body(not(containsString("metric_none")))
                .body(containsString(
                        "metric_all_total{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"none\",extra=\"tag\",method=\"countAllInvocations\",registry=\"prometheus\",result=\"success\",} 1.0"))
                .body(containsString(
                        "metric_all_total{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"NullPointerException\",extra=\"tag\",method=\"countAllInvocations\",registry=\"prometheus\",result=\"failure\",} 1.0"))
                .body(containsString(
                        "method_counted_total{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"NullPointerException\",method=\"emptyMetricName\",registry=\"prometheus\",result=\"failure\",} 1.0"))
                .body(containsString(
                        "method_counted_total{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"none\",method=\"emptyMetricName\",registry=\"prometheus\",result=\"success\",} 1.0"))
                .body(not(containsString("async_none")))
                .body(containsString(
                        "async_all_total{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"NullPointerException\",extra=\"tag\",method=\"countAllAsyncInvocations\",registry=\"prometheus\",result=\"failure\",} 1.0"))
                .body(containsString(
                        "async_all_total{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"none\",extra=\"tag\",method=\"countAllAsyncInvocations\",registry=\"prometheus\",result=\"success\",} 1.0"))
                .body(containsString(
                        "method_counted_total{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"NullPointerException\",method=\"emptyAsyncMetricName\",registry=\"prometheus\",result=\"failure\",} 1.0"))
                .body(containsString(
                        "method_counted_total{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"none\",method=\"emptyAsyncMetricName\",registry=\"prometheus\",result=\"success\",} 1.0"))

                // Annotated Timers
                .body(containsString(
                        "call_seconds_count{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"NullPointerException\",extra=\"tag\",method=\"call\",registry=\"prometheus\",} 1.0"))
                .body(containsString(
                        "call_seconds_count{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"none\",extra=\"tag\",method=\"call\",registry=\"prometheus\",}"))
                .body(containsString(
                        "async_call_seconds_count{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"NullPointerException\",extra=\"tag\",method=\"asyncCall\",registry=\"prometheus\",} 1.0"))
                .body(containsString(
                        "async_call_seconds_count{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",exception=\"none\",extra=\"tag\",method=\"asyncCall\",registry=\"prometheus\",} 1.0"))
                .body(containsString(
                        "longCall_seconds_active_count{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",extra=\"tag\",method=\"longCall\",registry=\"prometheus\",}"))
                .body(containsString(
                        "async_longCall_seconds_duration_sum{class=\"io.quarkus.it.micrometer.prometheus.AnnotatedResource\",env=\"test\",extra=\"tag\",method=\"longAsyncCall\",registry=\"prometheus\",} 0.0"))

                // Configured median, 95th percentile and histogram buckets
                .body(containsString(
                        "prime_number_test_seconds{env=\"test\",registry=\"prometheus\",quantile=\"0.5\",}"))
                .body(containsString(
                        "prime_number_test_seconds{env=\"test\",registry=\"prometheus\",quantile=\"0.95\",}"))
                .body(containsString(
                        "prime_number_test_seconds_bucket{env=\"test\",registry=\"prometheus\",le=\"0.001\",}"))

                // this was defined by a tag to a non-matching registry, and should not be found
                .body(not(containsString("class-should-not-match")))

                // should not find this ignored uri
                .body(not(containsString("uri=\"/fruit/create\"")));
    }
}
