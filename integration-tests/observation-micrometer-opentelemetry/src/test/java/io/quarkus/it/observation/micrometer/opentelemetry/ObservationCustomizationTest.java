package io.quarkus.it.observation.micrometer.opentelemetry;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
@TestProfile(CustomizationProfile.class)
public class ObservationCustomizationTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @BeforeEach
    void reset() {
        given().get("/reset").then().statusCode(200);
        spanExporter().reset();
    }

    @Test
    void filterAddsKeyValue() {
        given()
                .get("/observation/manual")
                .then()
                .statusCode(200)
                .body(is("manual-result"));

        // CustomObservationFilter adds "filtered.by=custom-filter" to all observations
        spanExporter().awaitUntil("a span with the attribute filtered.by=custom-filter", Duration.ofSeconds(5),
                spans -> !spans.filter(span -> "custom-filter".equals(span.getAttributes().get(stringKey("filtered.by"))))
                        .isEmpty());

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

        SpanDataList allSpans = spanExporter().fetch();
        assertThat(allSpans.named("ignored.operation")).isEmpty();
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
        SpanDataList spans = getSpans(1);
        assertThat(spans.named("custom-doWork")).hasSize(1);

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

    private SpanDataList getSpans(int minCount) {
        return spanExporter().awaitUntil("at least " + minCount + " spans", Duration.ofSeconds(5),
                spans -> spans.size() >= minCount);
    }

    private List<Map<String, Object>> getMetrics(String metricName) {
        return given()
                .queryParam("name", metricName)
                .get("/export/metrics")
                .body().as(new TypeRef<>() {
                });
    }
}
