package io.quarkus.it.opentelemetry.devservices;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import io.quarkus.it.opentelemetry.devservices.helper.JaegerCollectorClient;
import io.quarkus.it.opentelemetry.devservices.helper.JaegerCollectorResponse;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisabledOnOs({ OS.WINDOWS, OS.MAC })
public class OpenTelemetryDevservicesTestCase {

    @Inject
    @RestClient
    JaegerCollectorClient client;

    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    // We need this to ignore any previous tracing.
    // Jaeger expects unix time in microseconds.
    private long startTimeInMicro = 0L;

    @BeforeEach
    public void getStartTime() {
        startTimeInMicro = System.currentTimeMillis() * 1000L;
    }

    private List<String> getServices() {
        return client.getServices().getServices();
    }

    private List<JaegerCollectorResponse.Process> getProcesses() {
        return client.getTracedService(applicationName, startTimeInMicro).getData();
    }

    private List<JaegerCollectorResponse.Process.Span> getSpans() {
        return getProcesses().stream()
                .filter(
                        process -> process.getSpans().stream()
                                .anyMatch(span -> span.getTags().stream()
                                        .anyMatch(tag -> tag.getKey().contains("net.host.port")
                                                && tag.getValue().contains("8081"))))
                .findAny()
                .orElseThrow(IllegalStateException::new)
                .getSpans();
    }

    private JaegerCollectorResponse.Process.Span getLastSpan() {
        return getSpans().stream().max(Comparator.comparing(JaegerCollectorResponse.Process.Span::getStartTime))
                .orElseThrow(NoSuchElementException::new);
    }

    private int getSpansCount() {
        if (getServices() == null ||
                !getServices().contains(applicationName) ||
                getProcesses().size() == 0)
            return 0;

        try {
            return getSpans().size();
        } catch (IllegalStateException e) {
            // waiting for spans to be exported
        }
        return 0;
    }

    @Test
    void testResourceTracing() {

        int spansCount = getSpansCount();

        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() == spansCount + 1);
        Assertions.assertTrue(getServices().contains(applicationName));

        JaegerCollectorResponse.Process.Span spanData = getLastSpan();
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.getSpanID());
        Assertions.assertNotNull(spanData.getTraceID());
        Assertions.assertEquals("GET /direct", spanData.getOperationName());

        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("http.method") && tag.getValue().equals("GET")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("http.status_code") && tag.getValue().equals("200")));
    }

    @Test
    void testChainedResourceTracing() {

        int spansCount = getSpansCount();

        given()
                .contentType("application/json")
                .when().get("/chained")
                .then()
                .statusCode(200)
                .body("message", equalTo("Chained trace"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() == spansCount + 1);
        JaegerCollectorResponse.Process.Span spanData = getLastSpan();
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.getSpanID());
        Assertions.assertNotNull(spanData.getTraceID());
        Assertions.assertEquals("GET /chained", spanData.getOperationName());

        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("http.method") && tag.getValue().equals("GET")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("http.status_code") && tag.getValue().equals("200")));
    }

    @Test
    void testDeepPathNaming() {

        int spansCount = getSpansCount();

        given()
                .contentType("application/json")
                .when().get("/deep/path")
                .then()
                .statusCode(200)
                .body("message", equalTo("Deep url path"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() == spansCount + 1);
        JaegerCollectorResponse.Process.Span spanData = getLastSpan();
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.getSpanID());
        Assertions.assertNotNull(spanData.getTraceID());
        Assertions.assertEquals("GET /deep/path",
                spanData.getOperationName());

        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("http.method") && tag.getValue().equals("GET")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("http.status_code") && tag.getValue().equals("200")));
    }

    @Test
    void testPathParameter() {

        int spansCount = getSpansCount();

        given()
                .contentType("application/json")
                .when().get("/param/12345")
                .then()
                .statusCode(200)
                .body("message", equalTo("ParameterId: 12345"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() == spansCount + 1);
        JaegerCollectorResponse.Process.Span spanData = getLastSpan();
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.getSpanID());
        Assertions.assertNotNull(spanData.getTraceID());
        Assertions.assertEquals("GET /param/{paramId}", spanData.getOperationName());

        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("http.method") && tag.getValue().equals("GET")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("http.status_code") && tag.getValue().equals("200")));
    }

    @Test
    void testClientTracing() {

        int spansCount = getSpansCount();

        given()
                .when().get("/client/ping/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() == spansCount + 3);

        boolean outsideServerFound = false;
        boolean clientFound = false;

        for (JaegerCollectorResponse.Process.Span spanData : getSpans()) {
            Assertions.assertNotNull(spanData);

            if (spanData.getTags().stream()
                    .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server"))) {

                if (spanData.getOperationName().equals("GET /client/ping/{message}")) {
                    outsideServerFound = true;
                } else if (spanData.getOperationName().equals("GET /client/pong/{message}")) {
                    clientFound = true;
                }

                Assertions.assertNotNull(spanData.getSpanID());
                Assertions.assertNotNull(spanData.getTraceID());

                Assertions.assertTrue(spanData.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
                Assertions.assertTrue(spanData.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equals("http.method") && tag.getValue().equals("GET")));
                Assertions.assertTrue(spanData.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equals("http.status_code") && tag.getValue().equals("200")));
            }
        }

        Assertions.assertTrue(outsideServerFound);
        Assertions.assertTrue(clientFound);
    }

    @Test
    void testAsyncClientTracing() {

        int spansCount = getSpansCount();

        given()
                .when().get("/client/async-ping/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() == spansCount + 3);

        boolean outsideServerFound = false;
        boolean clientFound = false;

        for (JaegerCollectorResponse.Process.Span spanData : getSpans()) {
            Assertions.assertNotNull(spanData);

            if (spanData.getTags().stream()
                    .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server"))) {

                if (spanData.getOperationName().equals("GET /client/async-ping/{message}")) {
                    outsideServerFound = true;
                } else if (spanData.getOperationName().equals("GET /client/async-pong/{message}")) {
                    clientFound = true;
                }

                Assertions.assertNotNull(spanData.getSpanID());
                Assertions.assertNotNull(spanData.getTraceID());

                Assertions.assertTrue(spanData.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
                Assertions.assertTrue(spanData.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equals("http.method") && tag.getValue().equals("GET")));
                Assertions.assertTrue(spanData.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equals("http.status_code") && tag.getValue().equals("200")));
            }
        }

        Assertions.assertTrue(outsideServerFound);
        Assertions.assertTrue(clientFound);
    }
}
