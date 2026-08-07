package io.quarkus.it.observation.prometheus;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ObservationPrometheusTest {

    @BeforeEach
    void reset() {
        given().get("/reset").then().statusCode(200);
    }

    @Test
    void observationProducesSpan() {
        given()
                .get("/observation/manual")
                .then()
                .statusCode(200)
                .body(is("manual-result"));

        List<Map<String, Object>> spans = getSpans(1);
        Map<String, Object> span = findSpanByName(spans, "manual.operation");
        assertThat(span).isNotNull();

        await().atMost(5, SECONDS).untilAsserted(() -> {
            String body = get("/q/metrics").then().extract().asString();
            assertTrue(body.contains(
                    "manual_operation_seconds_count{error=\"none\",operation_type=\"manual\"} 1.0"), body);
            assertTrue(body.contains(
                    "manual_operation_seconds_sum{error=\"none\",operation_type=\"manual\"}"), body);
            assertTrue(body.contains(
                    "manual_operation_seconds_max{error=\"none\",operation_type=\"manual\"}"), body);
        });
    }

    @Test
    void observedInterceptorProducesSpanAndMetrics() {
        given()
                .get("/observation/observed")
                .then()
                .statusCode(200)
                .body(is("observed-result"));

        List<Map<String, Object>> spans = getSpans(1);
        Map<String, Object> span = findSpanByName(spans, "ObservedService#doWork");
        assertThat(span).isNotNull();

        await().atMost(5, SECONDS).untilAsserted(() -> {
            String body = get("/q/metrics").then().extract().asString();
            assertTrue(body.contains(
                    "doWork_seconds_count{code_function=\"doWork\",code_namespace=" +
                            "\"io.quarkus.it.observation.prometheus.ObservedService\",error=\"none\"} 1.0"),
                    body);
            assertTrue(body.contains(
                    "doWork_seconds_sum{code_function=\"doWork\",code_namespace=" +
                            "\"io.quarkus.it.observation.prometheus.ObservedService\",error=\"none\"}"),
                    body);
            assertTrue(body.contains(
                    "doWork_seconds_max{code_function=\"doWork\",code_namespace=" +
                            "\"io.quarkus.it.observation.prometheus.ObservedService\",error=\"none\"}"),
                    body);
        });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSpans(int minCount) {
        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<Map<String, Object>> result = get("/export").as(List.class);
            assertThat(result).hasSizeGreaterThanOrEqualTo(minCount);
        });
        return get("/export").as(List.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findSpanByName(List<Map<String, Object>> spans, String name) {
        return spans.stream()
                .filter(s -> name.equals(s.get("name")))
                .findFirst()
                .orElse(null);
    }
}
