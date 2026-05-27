package io.quarkus.it.micrometer.security;

import static io.quarkus.test.micrometer.PrometheusMetricsAssert.assertMetrics;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SecuredResourceTest {

    @Test
    void testMetricsForUnauthorizedRequest() {
        when().get("/secured/foo")
                .then()
                .statusCode(403);

        assertMetrics(when().get("/q/metrics").then().statusCode(200)
                .extract().asInputStream())
                .doesNotHaveMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/secured/foo"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/secured/{message}"));
    }

    @Test
    void testMetricsForUnauthorizedRequestWithOverlappingPaths() {
        // When two controllers have overlapping paths (e.g. /secured/{message} and /secured/{message}/details),
        // the URI template should still be resolved correctly for unauthorized requests.
        // See https://github.com/quarkusio/quarkus/issues/53030
        when().get("/secured/foo")
                .then()
                .statusCode(403);

        when().get("/secured/foo/details")
                .then()
                .statusCode(403);

        assertMetrics(when().get("/q/metrics").then().statusCode(200)
                .extract().asInputStream())
                .doesNotHaveMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/secured/foo"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/secured/{message}"))
                .hasMetricWithLabels("http_server_requests_seconds_count",
                        entry("uri", "/secured/{message}/details"));
    }

}
