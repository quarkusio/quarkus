package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class MetricsTest {
    @BeforeEach
    @AfterEach
    void reset() {
        await().atMost(5, SECONDS).until(() -> {
            // make sure spans are cleared
            List<Map<String, Object>> spans = getSpans();
            if (!spans.isEmpty()) {
                given().get("/reset").then().statusCode(HTTP_OK);
            }
            return spans.isEmpty();
        });
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    private List<Map<String, Object>> getMetrics(String metricName) {
        return given()
                .when()
                .queryParam("name", metricName)
                .get("/export/metrics")
                .body().as(new TypeRef<>() {
                });
    }

    @Test
    public void directCounterTest() {
        given()
                .when()
                .get("/direct-metrics")
                .then()
                .statusCode(200);
        given()
                .when().get("/direct-metrics")
                .then()
                .statusCode(200);

        await().atMost(10, SECONDS).until(() -> getSpans().size() >= 2);
        assertEquals(2, getSpans().size(), () -> "The spans are " + getSpans());
        await().atMost(10, SECONDS).until(() -> getMetrics("direct-trace-counter").size() > 2);

        List<Map<String, Object>> metrics = getMetrics("direct-trace-counter");
        Integer value = (Integer) ((Map) ((List) ((Map) (getMetrics("direct-trace-counter")
                .get(metrics.size() - 1)
                .get("longSumData")))
                .get("points"))
                .get(0))
                .get("value");

        assertEquals(2, value, "received: " + given()
                .when()
                .get("/export/metrics")
                .body().as(new TypeRef<>() {
                }));
    }

    @Test
    void testJvmMetrics() {
        await().atMost(10, SECONDS).until(() -> getMetrics("jvm.thread.count").size() > 2);

        List<Map<String, Object>> metrics = getMetrics("jvm.thread.count");

        Integer value = (Integer) ((Map) ((List) ((Map) (getMetrics("jvm.thread.count")
                .get(metrics.size() - 1)
                .get("longSumData")))
                .get("points"))
                .get(0))
                .get("value");

        assertThat(value, greaterThan(0)); // at least one thread is running
    }

    @Test
    void testServerRequestDuration() {
        given()
                .when()
                .get("/nopath")
                .then()
                .statusCode(200);

        await().atMost(10, SECONDS).until(() -> getMetrics("http.server.request.duration").size() > 2);

        List<Map<String, Object>> metrics = getMetrics("http.server.request.duration");

        Integer value = (Integer) ((Map) ((List) ((Map) (getMetrics("http.server.request.duration")
                .get(metrics.size() - 1)
                .get("data")))
                .get("points"))
                .get(0))
                .get("count");

        assertThat(value, greaterThanOrEqualTo(1)); // at least one endpoint was called once
    }
}
