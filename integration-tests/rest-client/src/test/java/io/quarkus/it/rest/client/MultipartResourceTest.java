package io.quarkus.it.rest.client;

import static io.quarkus.test.micrometer.PrometheusMetricsAssert.assertMetrics;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MultipartResourceTest {

    @Test
    public void testMultipartDataIsSent() {
        given()
                .header("Content-Type", "text/plain")
                .when().post("/client/multipart")
                .then()
                .statusCode(200)
                .body(containsString("Content-Disposition: form-data; name=\"file\""),
                        containsString("HELLO WORLD"),
                        containsString("Content-Disposition: form-data; name=\"fileName\""),
                        containsString("greeting.txt"),
                        containsString("Content-Disposition: form-data; name=\"uuid\""));

        assertMetrics(given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .extract().asInputStream())
                .doesNotHaveMetricWithLabels("http_client_requests_seconds_count",
                        entry("clientName", "localhost"), entry("method", "POST"),
                        entry("outcome", "SUCCESS"), entry("status", "200"), entry("uri", "/echo"));
    }
}
