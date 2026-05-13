package io.quarkus.it.rest.client.wronghost;

import static io.quarkus.test.micrometer.PrometheusMetricsAssert.assertMetrics;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public abstract class BaseExternalWrongHostTestCase {

    @Test
    public void restClient() {
        when()
                .get("/wrong-host")
                .then()
                .statusCode(200)
                .body(is("200"));

        assertMetrics(when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .extract().asInputStream())
                .hasMetricWithLabels("http_client_requests_seconds_count",
                        entry("clientName", "localhost"), entry("method", "GET"),
                        entry("outcome", "SUCCESS"), entry("status", "200"), entry("uri", "root"));
    }
}
