package io.quarkus.it.opentelemetry;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.UrlAttributes.URL_SCHEME;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class LoggingResourceTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

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
        Map<String, Object> logLineSpanContext = (Map<String, Object>) logLine.get("spanContext");

        String spanId = (String) logLineSpanContext.get("spanId");

        SpanDataList spans = spanExporter().awaitUntil("at least 1 span", Duration.ofSeconds(30), s -> !s.isEmpty());
        SpanData spanData = spans.filter(span -> spanId.equals(span.getSpanId())).stream().findFirst().orElseThrow();

        assertNotNull(spanData);
        assertThat(spanData).hasNoParent();
        assertEquals(SpanId.getInvalid(), spanData.getParentSpanContext().getSpanId());
        assertEquals(true, logLineSpanContext.get("sampled"));

        assertEquals("INFO", logLine.get("severityText"));
        assertEquals(spanData.getTraceId(), logLineSpanContext.get("traceId"),
                "traceId do not match. SpanDataList: " + spans + " :::: logLineSpanContext: " + logLineSpanContext);
        assertEquals(spanData.getSpanId(), logLineSpanContext.get("spanId"));
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
        Map<String, Object> logLineSpanContext = (Map<String, Object>) logLine.get("spanContext");

        String spanId = (String) logLineSpanContext.get("spanId");

        SpanDataList spans = spanExporter().awaitUntil("at least 1 span", Duration.ofSeconds(30), s -> !s.isEmpty());

        SpanData spanData = spans.filter(span -> spanId.equals(span.getSpanId())).stream().findFirst().orElseThrow();

        assertNotNull(spanData);
        assertThat(spanData).hasNoParent();
        assertEquals(SpanId.getInvalid(), spanData.getParentSpanContext().getSpanId());
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

        final SpanDataList spans = spanExporter().awaitCount(2, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        // Server span
        final SpanData server = spans.ofKind(SERVER).roots().single();
        assertThat(server).hasName("GET /chained");
        assertEquals(SpanId.getInvalid(), server.getParentSpanContext().getSpanId());
        assertThat(server)
                .hasAttribute(SERVER_ADDRESS, deepPathUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) deepPathUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);

        // Wait for logs to be available as everything is async
        await().atMost(Duration.ofSeconds(5)).until(() -> getLogs("chainedTrace called").size() == 1);
        final Map<String, Object> serverLine = getLogs("chainedTrace called").get(0);

        final Map<String, Object> serverLineSpanContext = (Map<String, Object>) serverLine.get("spanContext");
        assertEquals(server.getTraceId(), serverLineSpanContext.get("traceId"));
        assertEquals(server.getSpanId(), serverLineSpanContext.get("spanId"));
        assertEquals(true, serverLineSpanContext.get("sampled"));
        assertEquals("INFO", serverLine.get("severityText"));

        // chained CDI call
        final SpanData cdi = spans.ofKind(INTERNAL).childrenOf(server).single();
        assertThat(cdi)
                .hasName("TracedService.call")
                .hasParent(server);

        await().atMost(Duration.ofSeconds(5)).until(() -> getLogs("Chained trace called").size() == 1);
        final Map<String, Object> cdiLine = getLogs("Chained trace called").get(0);

        final Map<String, Object> cdiLineSpanContext = (Map<String, Object>) cdiLine.get("spanContext");
        assertEquals(cdi.getTraceId(), cdiLineSpanContext.get("traceId"));
        assertEquals(cdi.getSpanId(), cdiLineSpanContext.get("spanId"));
        assertEquals(true, cdiLineSpanContext.get("sampled"));
        assertEquals("INFO", cdiLine.get("severityText"));
    }
}
