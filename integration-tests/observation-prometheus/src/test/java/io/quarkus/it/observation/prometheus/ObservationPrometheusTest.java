package io.quarkus.it.observation.prometheus;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;

@QuarkusTest
public class ObservationPrometheusTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @BeforeEach
    void reset() {
        spanExporter().reset();
    }

    @Test
    void observationProducesSpan() {
        given()
                .get("/observation/manual")
                .then()
                .statusCode(200)
                .body(is("manual-result"));

        SpanDataList spans = getSpans(1);
        assertThat(spans.named("manual.operation")).hasSize(1);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            String body = get("/q/metrics").then().extract().asString();
            assertThat(body).contains(
                    "manual_operation_seconds_count{error=\"none\",operation_type=\"manual\"} 1");
            assertThat(body).contains(
                    "manual_operation_seconds_sum{error=\"none\",operation_type=\"manual\"}");
            assertThat(body).contains(
                    "manual_operation_seconds_max{error=\"none\",operation_type=\"manual\"}");
        });
    }

    @Test
    void observedInterceptorProducesSpanAndMetrics() {
        given()
                .get("/observation/observed")
                .then()
                .statusCode(200)
                .body(is("observed-result"));

        SpanDataList spans = getSpans(1);
        assertThat(spans.named("ObservedService#doWork")).hasSize(1);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            String body = get("/q/metrics").then().extract().asString();
            assertThat(body).contains(
                    "doWork_seconds_count{code_function=\"doWork\",code_namespace=" +
                            "\"io.quarkus.it.observation.prometheus.ObservedService\",error=\"none\"} 1");
            assertThat(body).contains(
                    "doWork_seconds_sum{code_function=\"doWork\",code_namespace=" +
                            "\"io.quarkus.it.observation.prometheus.ObservedService\",error=\"none\"}");
            assertThat(body).contains(
                    "doWork_seconds_max{code_function=\"doWork\",code_namespace=" +
                            "\"io.quarkus.it.observation.prometheus.ObservedService\",error=\"none\"}");
        });
    }

    private SpanDataList getSpans(int minCount) {
        return spanExporter().awaitUntil("at least " + minCount + " spans", Duration.ofSeconds(5),
                spans -> spans.size() >= minCount);
    }
}
