package io.quarkus.it.observation.micrometer.opentelemetry;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class ObservationMicrometerOtelTest {

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
    void manualObservationProducesSpanAndOtelMetrics() {
        given()
                .get("/observation/manual")
                .then()
                .statusCode(200)
                .body(is("manual-result"));

        SpanDataList spans = getSpans(1);
        SpanData span = spans.named("manual.operation").single();
        assertThat(span).hasAttribute(stringKey("operation.type"), "manual");

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

        SpanDataList spans = getSpans(1);
        assertThat(spans.named("ObservedService#doWork")).hasSize(1);

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
