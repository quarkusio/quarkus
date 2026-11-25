package io.quarkus.it.opentelemetry.spi;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.ClientAttributes.CLIENT_ADDRESS;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_LANGUAGE;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_NAME;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_VERSION;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_SCHEME;
import static io.opentelemetry.semconv.UserAgentAttributes.USER_AGENT_ORIGINAL;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanExporterClient;

@QuarkusTest
public class OTelSpiTest {

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
        spanExporter().reset();
    }

    @Test
    void testResourceTracing() {
        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        SpanData span = spanExporter().awaitCount(1, Duration.ofMinutes(2)).single();
        assertNotNull(span);
        assertNotNull(span.getSpanId());

        verifyResource(span);

        assertThat(span)
                .hasName("GET /direct")
                .hasKind(SERVER);
        assertTrue(span.hasEnded());

        assertEquals(SpanId.getInvalid(), span.getParentSpanContext().getSpanId());
        assertThat(span).hasNoParent();
        assertFalse(span.getParentSpanContext().isRemote());

        assertThat(span)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/direct")
                .hasAttribute(SERVER_ADDRESS, deepPathUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) deepPathUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(span.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(span.getAttributes().get(USER_AGENT_ORIGINAL));
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

        SpanData span = spanExporter().awaitCount(1, Duration.ofMinutes(2)).single();
        assertNotNull(span);
        assertNotNull(span.getSpanId());

        verifyResource(span);

        // /param/67 was sampled
        assertThat(span).hasName("GET /direct");

    }

    private void verifyResource(SpanData span) {
        assertEquals("opentelemetry-integration-test-spi", span.getResource().getAttribute(SERVICE_NAME));
        assertEquals("999-SNAPSHOT", span.getResource().getAttribute(SERVICE_VERSION));
        assertEquals("java", span.getResource().getAttribute(TELEMETRY_SDK_LANGUAGE));
        assertEquals("opentelemetry", span.getResource().getAttribute(TELEMETRY_SDK_NAME));
        assertNotNull(span.getResource().getAttribute(TELEMETRY_SDK_VERSION));
    }
}
