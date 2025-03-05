package io.quarkus.micrometer.opentelemetry;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class MicrometerCounterInterceptorTest {

    @BeforeEach
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
    }

    @Test
    void testCountAllMetrics_MetricsOnSuccess() {
        given()
                .when()
                .get("/count")
                .then()
                .statusCode(200);

        await().atMost(5, SECONDS).until(() -> getMetrics("metric.all").size() > 1);

        List<Map<String, Object>> metrics = getMetrics("metric.all");

        Double value = (Double) ((Map) ((List) ((Map) (getMetrics("metric.all")
                .get(metrics.size() - 1)
                .get("data")))
                .get("points"))
                .get(0))
                .get("value");
        assertThat(value).isEqualTo(1d);
    }

    private List<Map<String, Object>> getMetrics(String metricName) {
        return given()
                .when()
                .queryParam("name", metricName)
                .get("/export/metrics")
                .body().as(new TypeRef<>() {
                });
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }
}
