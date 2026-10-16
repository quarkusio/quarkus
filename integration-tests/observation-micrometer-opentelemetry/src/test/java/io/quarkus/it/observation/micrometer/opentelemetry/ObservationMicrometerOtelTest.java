package io.quarkus.it.observation.micrometer.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class ObservationMicrometerOtelTest {

    @BeforeEach
    void reset() {
        given().get("/reset").then().statusCode(200);
    }

    @Test
    void manualObservationProducesSpanAndOtelMetrics() {
        given()
                .get("/observation/manual")
                .then()
                .statusCode(200)
                .body(is("manual-result"));

        List<Map<String, Object>> spans = getSpans(1);
        Map<String, Object> span = findSpanByName(spans, "manual.operation");
        assertThat(span).isNotNull();
        Map attributes = (Map) span.get("attributes");
        assertThat(attributes.get("operation.type")).isEqualTo("manual");

        await().atMost(5, SECONDS).untilAsserted(() -> {
            long count = getTimerCount("manual.operation");
            assertThat(count).isEqualTo(1);
        });
    }

    @Test
    void observedInterceptorProducesSpanAndOtelMetrics() {
        given()
                .get("/observation/observed")
                .then()
                .statusCode(200)
                .body(is("observed-result"));

        List<Map<String, Object>> spans = getSpans(1);
        Map<String, Object> span = findSpanByName(spans, "ObservedService#doWork");
        assertThat(span).isNotNull();

        await().atMost(5, SECONDS).untilAsserted(() -> {
            long count = getTimerCount("doWork");
            assertThat(count).isEqualTo(1);
        });
    }

    @SuppressWarnings("unchecked")
    private long getTimerCount(String metricName) {
        List<Map<String, Object>> metrics = getMetrics(metricName);
        assertThat(metrics).isNotEmpty();
        Map<String, Object> latest = metrics.get(metrics.size() - 1);
        Map<String, Object> data = (Map<String, Object>) latest.get("data");
        List<Map<String, Object>> points = (List<Map<String, Object>>) data.get("points");
        assertThat(points).isNotEmpty();
        return ((Number) points.get(0).get("count")).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSpans(int minCount) {
        await().atMost(5, SECONDS).untilAsserted(() -> {
            List<Map<String, Object>> result = get("/export").as(List.class);
            assertThat(result).hasSizeGreaterThanOrEqualTo(minCount);
        });
        return get("/export").as(List.class);
    }

    private List<Map<String, Object>> getMetrics(String metricName) {
        return given()
                .queryParam("name", metricName)
                .get("/export/metrics")
                .body().as(new TypeRef<>() {
                });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findSpanByName(List<Map<String, Object>> spans, String name) {
        return spans.stream()
                .filter(s -> name.equals(s.get("name")))
                .findFirst()
                .orElse(null);
    }
}
