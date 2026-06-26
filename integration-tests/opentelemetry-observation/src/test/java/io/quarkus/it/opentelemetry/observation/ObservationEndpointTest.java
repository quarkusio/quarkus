package io.quarkus.it.opentelemetry.observation;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ObservationEndpointTest {

    @BeforeEach
    void reset() {
        given().get("/reset").then().statusCode(200);
    }

    @Test
    void manualObservationProducesSpan() {
        given()
                .get("/observation/manual")
                .then()
                .statusCode(200);

        List<Map<String, Object>> spans = getSpans(1);
        Map<String, Object> span = findSpanByName(spans, "manual.operation");
        assertThat(span).isNotNull();
    }

    @Test
    void nestedObservationsProduceParentChildSpans() {
        given()
                .get("/observation/nested")
                .then()
                .statusCode(200);

        List<Map<String, Object>> spans = getSpans(2);
        Map<String, Object> parentSpan = findSpanByName(spans, "parent.operation");
        Map<String, Object> childSpan = findSpanByName(spans, "child.operation");

        assertThat(parentSpan).isNotNull();
        assertThat(childSpan).isNotNull();
        assertThat(childSpan.get("traceId")).isEqualTo(parentSpan.get("traceId"));
        assertThat(childSpan.get("parentSpanId")).isEqualTo(parentSpan.get("spanId"));
    }

    @Test
    void observedInterceptorProducesSpan() {
        given()
                .get("/observation/observed")
                .then()
                .statusCode(200);

        List<Map<String, Object>> spans = getSpans(1);
        Map<String, Object> span = findSpanByName(spans, "ObservedService#doWork");
        assertThat(span).isNotNull();
    }

    @Test
    void errorObservationProducesErrorSpan() {
        given()
                .get("/observation/error")
                .then()
                .statusCode(200);

        List<Map<String, Object>> spans = getSpans(1);
        Map<String, Object> span = findSpanByName(spans, "error.operation");
        assertThat(span).isNotNull();
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
