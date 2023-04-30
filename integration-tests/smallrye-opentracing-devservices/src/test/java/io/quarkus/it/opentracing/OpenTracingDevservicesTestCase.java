package io.quarkus.it.opentracing;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import io.quarkus.it.opentracing.helper.JaegerCollectorClient;
import io.quarkus.it.opentracing.helper.JaegerCollectorResponse;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisabledOnOs({ OS.WINDOWS, OS.MAC })
public class OpenTracingDevservicesTestCase {

    @Inject
    @RestClient
    JaegerCollectorClient client;

    // We need this to ignore any previous tracing.
    // Jaeger expects unix time in microseconds.
    private long startTimeInMicro = 0l;

    @BeforeEach
    public void getStartTime() {
        startTimeInMicro = System.currentTimeMillis() * 1000L;
    }

    private List<String> getServices() {
        return client.getServices().getServices();
    }

    private List<JaegerCollectorResponse.Process> getProcesses() {
        return client.getTracedService(getServiceName(), startTimeInMicro).getData();
    }

    private JaegerCollectorResponse.Process.Span getLastSpan() {
        JaegerCollectorResponse.Process process = getProcesses().get(0);
        return process.getSpans().get(process.getSpans().size() - 1);
    }

    private String getServiceName() {
        return ConfigProvider.getConfig().getValue("quarkus.jaeger.service-name", String.class);
    }

    private int getSpansCount() {
        if (getServices() == null ||
                !getServices().contains(getServiceName()) ||
                getProcesses().size() == 0)
            return 0;

        return getProcesses().get(0).getSpans().size();
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

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() > spansCount);
        Assertions.assertTrue(getServices().contains(getServiceName()));

        JaegerCollectorResponse.Process.Span spanData = getLastSpan();
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.getSpanID());
        Assertions.assertNotNull(spanData.getTraceID());
        Assertions.assertEquals("GET:io.quarkus.it.opentracing.SimpleResource.directTrace", spanData.getOperationName());

        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("component") && tag.getValue().equals("jaxrs")));
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

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() > spansCount);
        JaegerCollectorResponse.Process.Span spanData = getLastSpan();
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.getSpanID());
        Assertions.assertNotNull(spanData.getTraceID());
        Assertions.assertEquals("GET:io.quarkus.it.opentracing.SimpleResource.chainedTrace", spanData.getOperationName());

        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("component") && tag.getValue().equals("jaxrs")));
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

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() > spansCount);
        JaegerCollectorResponse.Process.Span spanData = getLastSpan();
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.getSpanID());
        Assertions.assertNotNull(spanData.getTraceID());
        Assertions.assertEquals("GET:io.quarkus.it.opentracing.SimpleResource.deepUrlPathTrace",
                spanData.getOperationName());

        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("component") && tag.getValue().equals("jaxrs")));
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

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() > spansCount);
        JaegerCollectorResponse.Process.Span spanData = getLastSpan();
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.getSpanID());
        Assertions.assertNotNull(spanData.getTraceID());
        Assertions.assertEquals("GET:io.quarkus.it.opentracing.SimpleResource.pathParameters", spanData.getOperationName());

        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
        Assertions.assertTrue(spanData.getTags().stream()
                .anyMatch(tag -> tag.getKey().equals("component") && tag.getValue().equals("jaxrs")));
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

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() > spansCount);

        boolean outsideServerFound = false;
        boolean clientFound = false;

        for (JaegerCollectorResponse.Process.Span spanData : getProcesses().get(0).getSpans()) {
            Assertions.assertNotNull(spanData);

            if (spanData.getTags().stream()
                    .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server"))) {

                if (spanData.getOperationName().equals("GET:io.quarkus.it.opentracing.PingPongResource.ping")) {
                    outsideServerFound = true;
                } else if (spanData.getOperationName().equals("GET:io.quarkus.it.opentracing.PingPongResource.pong")) {
                    clientFound = true;
                }

                Assertions.assertNotNull(spanData.getSpanID());
                Assertions.assertNotNull(spanData.getTraceID());

                Assertions.assertTrue(spanData.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
                Assertions.assertTrue(spanData.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equals("component") && tag.getValue().equals("jaxrs")));
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

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> getSpansCount() > spansCount);

        boolean outsideServerFound = false;
        boolean clientFound = false;

        for (JaegerCollectorResponse.Process.Span spanData : getProcesses().get(0).getSpans()) {
            Assertions.assertNotNull(spanData);

            if (spanData.getTags().stream()
                    .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server"))) {

                if (spanData.getOperationName().equals("GET:io.quarkus.it.opentracing.PingPongResource.asyncPing")) {
                    outsideServerFound = true;
                } else if (spanData.getOperationName().equals("GET:io.quarkus.it.opentracing.PingPongResource.pong")) {
                    clientFound = true;
                }

                Assertions.assertNotNull(spanData.getSpanID());
                Assertions.assertNotNull(spanData.getTraceID());

                Assertions.assertTrue(spanData.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equals("span.kind") && tag.getValue().equals("server")));
                Assertions.assertTrue(spanData.getTags().stream()
                        .anyMatch(tag -> tag.getKey().equals("component") && tag.getValue().equals("jaxrs")));
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
