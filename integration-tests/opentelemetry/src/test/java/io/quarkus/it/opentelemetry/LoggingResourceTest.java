package io.quarkus.it.opentelemetry;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceId;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class LoggingResourceTest {

    @TestHTTPResource("deep/path")
    URL deepPathUrl;

    @BeforeEach
    @AfterEach
    void reset() {
        await().atMost(5, SECONDS).until(() -> {
            List<Map<String, Object>> logs = getLogs();
            if (logs.size() == 0) {
                return true;
            } else {
                given().get("/reset").then().statusCode(HTTP_OK);
                return false;
            }
        });
    }

    private List<Map<String, Object>> getLogs() {
        return get("/export/logs").body().as(new TypeRef<>() {
        });
    }

    private List<Map<String, Object>> getLogs(final String message) {
        return given()
                .when()
                .queryParam("body", message)
                .get("/export/logs")
                .body()
                .as(new TypeRef<>() {
                });
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    @Test
    public void testDirectEndpoint() {
        // This will create 1 log, but some logs could already exist.
        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        // Wait for logs to be available as everything is async
        await().atMost(Duration.ofSeconds(5)).until(() -> getLogs("directTrace called").size() == 1);
        Map<String, Object> logLine = getLogs("directTrace called").get(0);

        await().atMost(Duration.ofMinutes(2)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);

        assertEquals(SpanId.getInvalid(), spanData.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), spanData.get("parent_traceId"));

        assertEquals("INFO", logLine.get("severityText"));
        Map<String, Object> logLineSpanContext = (Map<String, Object>) logLine.get("spanContext");
        assertEquals(spanData.get("traceId"), logLineSpanContext.get("traceId"));
        assertEquals(spanData.get("spanId"), logLineSpanContext.get("spanId"));
    }

    @Test
    public void testException() {
        // This will create 1 log, but some logs could already exist.
        given()
                .when().get("/exception")
                .then()
                .statusCode(200)
                .body(is("Oh no! An exception"));

        // Wait for logs to be available as everything is async
        await().atMost(Duration.ofSeconds(5)).until(() -> getLogs("Oh no Exception!").size() == 1);
        Map<String, Object> logLine = getLogs("Oh no Exception!").get(0);

        await().atMost(Duration.ofMinutes(2)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);

        assertEquals(SpanId.getInvalid(), spanData.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), spanData.get("parent_traceId"));

        Map<String, Object> logLineSpanContext = (Map<String, Object>) logLine.get("spanContext");
        assertEquals(spanData.get("traceId"), logLineSpanContext.get("traceId"));
        assertEquals(spanData.get("spanId"), logLineSpanContext.get("spanId"));
        assertEquals(true, logLineSpanContext.get("sampled"));

        assertEquals("ERROR", logLine.get("severityText"));
        assertEquals("java.lang.RuntimeException", logLine.get("attr_exception.type"));
        assertTrue(((String) logLine.get("attr_exception.stacktrace"))
                .contains("java.lang.RuntimeException: Exception!"),
                " Stacktrace found: " + logLine.get("attr_exception.stacktrace"));
    }

    @Test
    void testChainedResourceTracing() {
        given()
                .contentType("application/json")
                .when().get("/chained")
                .then()
                .statusCode(200)
                .body("message", CoreMatchers.equalTo("Chained trace"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 2);
        final List<Map<String, Object>> spans = getSpans();
        assertEquals(2, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        // Server span
        final Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals(SERVER.toString(), server.get("kind"));
        assertEquals("GET /chained", server.get("name"));
        assertEquals(SERVER.toString(), server.get("kind"));
        assertEquals(SpanId.getInvalid(), server.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), server.get("parent_traceId"));
        assertEquals(deepPathUrl.getHost(), server.get("attr_server.address"));
        assertEquals(deepPathUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", server.get("attr_url.scheme"));
        assertEquals("200", server.get("attr_http.response.status_code"));

        // Wait for logs to be available as everything is async
        await().atMost(Duration.ofSeconds(5)).until(() -> getLogs("chainedTrace called").size() == 1);
        final Map<String, Object> serverLine = getLogs("chainedTrace called").get(0);

        final Map<String, Object> serverLineSpanContext = (Map<String, Object>) serverLine.get("spanContext");
        assertEquals(server.get("traceId"), serverLineSpanContext.get("traceId"));
        assertEquals(server.get("spanId"), serverLineSpanContext.get("spanId"));
        assertEquals(true, serverLineSpanContext.get("sampled"));
        assertEquals("INFO", serverLine.get("severityText"));

        // chained CDI call
        final Map<String, Object> cdi = getSpanByKindAndParentId(spans, INTERNAL, server.get("spanId"));
        assertEquals("TracedService.call", cdi.get("name"));
        assertEquals(SpanKind.INTERNAL.toString(), cdi.get("kind"));
        assertEquals(server.get("spanId"), cdi.get("parent_spanId"));

        await().atMost(Duration.ofSeconds(5)).until(() -> getLogs("Chained trace called").size() == 1);
        final Map<String, Object> cdiLine = getLogs("Chained trace called").get(0);

        final Map<String, Object> cdiLineSpanContext = (Map<String, Object>) cdiLine.get("spanContext");
        assertEquals(cdi.get("traceId"), cdiLineSpanContext.get("traceId"));
        assertEquals(cdi.get("spanId"), cdiLineSpanContext.get("spanId"));
        assertEquals(true, cdiLineSpanContext.get("sampled"));
        assertEquals("INFO", cdiLine.get("severityText"));
    }

    private static List<Map<String, Object>> getSpansByKindAndParentId(List<Map<String, Object>> spans, SpanKind kind,
            Object parentSpanId) {
        return spans.stream()
                .filter(map -> map.get("kind").equals(kind.toString()))
                .filter(map -> map.get("parentSpanId").equals(parentSpanId)).collect(toList());
    }

    private static Map<String, Object> getSpanByKindAndParentId(List<Map<String, Object>> spans, SpanKind kind,
            Object parentSpanId) {
        List<Map<String, Object>> span = getSpansByKindAndParentId(spans, kind, parentSpanId);
        assertEquals(1, span.size());
        return span.get(0);
    }
}
