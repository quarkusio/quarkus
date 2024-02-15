package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
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
        given().get("/reset").then().statusCode(HTTP_OK);
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    private List<Map<String, Object>> getMetrics() {
        return given()
                .when()
                .queryParam("name", "direct-trace-counter")
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

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 2);
        await().atMost(10, SECONDS).until(() -> getMetrics().size() > 2);

        List<Map<String, Object>> metrics = getMetrics();
        Integer value = (Integer) ((Map) ((List) ((Map) (getMetrics()
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
}
