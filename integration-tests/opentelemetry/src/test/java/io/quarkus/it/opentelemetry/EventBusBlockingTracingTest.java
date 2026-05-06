package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class EventBusBlockingTracingTest {

    @BeforeEach
    @AfterEach
    void reset() {
        await().atMost(5, SECONDS).until(() -> {
            List<Map<String, Object>> spans = getSpans();
            if (spans.size() == 0) {
                return true;
            } else {
                given().get("/reset").then().statusCode(HTTP_OK);
                return false;
            }
        });
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    @Test
    void blockingConsumeEventShouldPropagateTraceContext() {
        given().get("/eventbus-blocking/publish").then().statusCode(HTTP_OK);

        // Wait until the custom handler span appears (more reliable than counting total spans)
        await().atMost(Duration.ofSeconds(10)).until(() -> getSpans().stream()
                .anyMatch(s -> "inside-blocking-handler".equals(s.get("name"))));

        List<Map<String, Object>> spans = getSpans();

        Map<String, Object> httpSpan = spans.stream()
                .filter(s -> "SERVER".equals(s.get("kind")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No HTTP server span found. Spans: " + spans));

        String httpTraceId = (String) httpSpan.get("traceId");

        Map<String, Object> handlerSpan = spans.stream()
                .filter(s -> "inside-blocking-handler".equals(s.get("name")))
                .findFirst()
                .get();

        assertEquals(httpTraceId, handlerSpan.get("traceId"),
                "Span created inside blocking handler should have the same traceId as the HTTP span");
    }

    @Test
    void blockingAnnotationConsumeEventShouldPropagateTraceContext() {
        given().get("/eventbus-blocking/publish-annotation").then().statusCode(HTTP_OK);

        await().atMost(Duration.ofSeconds(10)).until(() -> getSpans().stream()
                .anyMatch(s -> "inside-blocking-annotation-handler".equals(s.get("name"))));

        List<Map<String, Object>> spans = getSpans();

        Map<String, Object> httpSpan = spans.stream()
                .filter(s -> "SERVER".equals(s.get("kind")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No HTTP server span found. Spans: " + spans));

        String httpTraceId = (String) httpSpan.get("traceId");

        Map<String, Object> handlerSpan = spans.stream()
                .filter(s -> "inside-blocking-annotation-handler".equals(s.get("name")))
                .findFirst()
                .get();

        assertEquals(httpTraceId, handlerSpan.get("traceId"),
                "Span created inside @Blocking handler should have the same traceId as the HTTP span");
    }
}
