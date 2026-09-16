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
import io.quarkus.test.junit.TestProfile;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@TestProfile(CustomizationProfile.class)
public class ObservationCustomizationTest {

    @BeforeEach
    void reset() {
        given().get("/reset").then().statusCode(200);
    }

    @Test
    void filterAddsKeyValue() {
        given()
                .get("/observation/manual")
                .then()
                .statusCode(200)
                .body(is("manual-result"));

        // CustomObservationFilter adds "filtered.by=custom-filter" to all observations
        await().atMost(5, SECONDS).untilAsserted(() -> {
            String body = get("/export").then().extract().asString();
            assertThat(body).contains("filtered.by");
            assertThat(body).contains("custom-filter");
        });

        await().atMost(5, SECONDS).untilAsserted(() -> {
            long count = getTimerCount("manual.operation");
            assertThat(count).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    void predicateSuppressesObservation() {
        given()
                .get("/observation/ignored")
                .then()
                .statusCode(200)
                .body(is("ignored-result"));

        // Give time for any spans/metrics to be exported
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<Map<String, Object>> metrics = getMetrics("ignored.operation");
        assertThat(metrics).isEmpty();

        List<Map<String, Object>> allSpans = getAllSpans();
        Map<String, Object> ignoredSpan = findSpanByName(allSpans, "ignored.operation");
        assertThat(ignoredSpan).isNull();
    }

    @Test
    void observedConventionCustomizesSpanName() {
        given()
                .get("/observation/observed")
                .then()
                .statusCode(200)
                .body(is("observed-result"));

        // CustomObservedConvention sets contextual name (span) to "custom-doWork"
        // and observation name (metric) to "custom.observed"
        List<Map<String, Object>> spans = getSpans(1);
        Map<String, Object> span = findSpanByName(spans, "custom-doWork");
        assertThat(span).isNotNull();

        await().atMost(5, SECONDS).untilAsserted(() -> {
            long count = getTimerCount("custom.observed");
            assertThat(count).isGreaterThanOrEqualTo(1);
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAllSpans() {
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
