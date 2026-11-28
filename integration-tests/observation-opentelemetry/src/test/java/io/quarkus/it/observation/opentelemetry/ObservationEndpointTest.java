package io.quarkus.it.observation.opentelemetry;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.restassured.RestAssured.given;

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
public class ObservationEndpointTest {

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
    void manualObservationProducesSpan() {
        given()
                .get("/observation/manual")
                .then()
                .statusCode(200);

        SpanDataList spans = getSpans(1);
        assertThat(spans.named("manual.operation")).hasSize(1);
    }

    @Test
    void nestedObservationsProduceParentChildSpans() {
        given()
                .get("/observation/nested")
                .then()
                .statusCode(200);

        SpanDataList spans = getSpans(2);
        SpanData parentSpan = spans.named("parent.operation").single();
        SpanData childSpan = spans.named("child.operation").single();

        assertThat(childSpan).hasTraceId(parentSpan.getTraceId()).hasParent(parentSpan);
    }

    @Test
    void observedInterceptorProducesSpan() {
        given()
                .get("/observation/observed")
                .then()
                .statusCode(200);

        SpanDataList spans = getSpans(1);
        assertThat(spans.named("ObservedService#doWork")).hasSize(1);
    }

    @Test
    void errorObservationProducesErrorSpan() {
        given()
                .get("/observation/error")
                .then()
                .statusCode(200);

        SpanDataList spans = getSpans(1);
        assertThat(spans.named("error.operation")).hasSize(1);
    }

    @Test
    void noObservationMetricsExportedViaOtel() {
        given()
                .get("/observation/manual")
                .then()
                .statusCode(200);

        // Wait for the span to be exported first
        getSpans(1);

        // Verify no observation metrics are exported via OTel (metrics exporter is disabled)
        List<Map<String, Object>> metrics = getMetrics("manual.operation");
        assertThat(metrics).isEmpty();

        metrics = getMetrics("service.work");
        assertThat(metrics).isEmpty();
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
