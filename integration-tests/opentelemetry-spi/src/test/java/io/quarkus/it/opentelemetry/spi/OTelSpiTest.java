package io.quarkus.it.opentelemetry.spi;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class OTelSpiTest {

    @TestHTTPResource("deep/path")
    URL deepPathUrl;

    @BeforeEach
    @AfterEach
    void reset() {
        await().atMost(Duration.ofSeconds(30L)).until(() -> {
            // make sure spans are cleared
            List<Map<String, Object>> spans = getSpans();
            if (!spans.isEmpty()) {
                given().get("/reset").then().statusCode(HTTP_OK);
            }
            return spans.isEmpty();
        });
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    @Test
    void testResourceTracing() {
        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        await().atMost(Duration.ofMinutes(2)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        assertNotNull(spanData);
        assertNotNull(spanData.get("spanId"));

        verifyResource(spanData);

        assertEquals("GET /direct", spanData.get("name"));
        assertEquals(SERVER.toString(), spanData.get("kind"));
        assertTrue((Boolean) spanData.get("ended"));

        assertEquals(SpanId.getInvalid(), spanData.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), spanData.get("parent_traceId"));
        assertFalse((Boolean) spanData.get("parent_valid"));
        assertFalse((Boolean) spanData.get("parent_remote"));

        assertEquals("GET", spanData.get("attr_http.request.method"));
        assertEquals("/direct", spanData.get("attr_url.path"));
        assertEquals(deepPathUrl.getHost(), spanData.get("attr_server.address"));
        assertEquals(deepPathUrl.getPort(), Integer.valueOf((String) spanData.get("attr_server.port")));
        assertEquals("http", spanData.get("attr_url.scheme"));
        assertEquals("200", spanData.get("attr_http.response.status_code"));
        assertNotNull(spanData.get("attr_client.address"));
        assertNotNull(spanData.get("attr_user_agent.original"));
    }

    @Test
    void testDropTracing() {
        given()
                .contentType("application/json")
                .when().get("/param/67")
                .then()
                .statusCode(200)
                .body("message", equalTo("ParameterId: 67"));

        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        await().atMost(Duration.ofMinutes(2)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        assertNotNull(spanData);
        assertNotNull(spanData.get("spanId"));

        verifyResource(spanData);

        // /param/67 was sampled
        assertEquals("GET /direct", spanData.get("name"));

    }

    private void verifyResource(Map<String, Object> spanData) {
        assertEquals("opentelemetry-integration-test-spi", spanData.get("resource_service.name"));
        assertEquals("999-SNAPSHOT", spanData.get("resource_service.version"));
        assertEquals("java", spanData.get("resource_telemetry.sdk.language"));
        assertEquals("opentelemetry", spanData.get("resource_telemetry.sdk.name"));
        assertNotNull(spanData.get("resource_telemetry.sdk.version"));
    }
}
