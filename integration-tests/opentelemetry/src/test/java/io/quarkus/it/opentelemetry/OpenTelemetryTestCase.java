package io.quarkus.it.opentelemetry;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class OpenTelemetryTestCase {
    @TestHTTPResource("direct")
    URL directUrl;

    @TestHTTPResource("chained")
    URL chainedUrl;

    @TestHTTPResource("deep/path")
    URL deepPathUrl;

    @TestHTTPResource("param")
    URL pathParamUrl;

    private void resetExporter() {
        given()
                .when().get("/export/clear")
                .then()
                .statusCode(204);
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<List<Map<String, Object>>>() {
        });
    }

    @Test
    void testResourceTracing() {
        resetExporter();

        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        Awaitility.await().atMost(Duration.ofMinutes(2)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.get("spanId"));

        verifyResource(spanData);

        Assertions.assertEquals("direct", spanData.get("name"));
        Assertions.assertEquals(SpanKind.SERVER.toString(), spanData.get("kind"));
        Assertions.assertTrue((Boolean) spanData.get("ended"));

        Assertions.assertEquals(SpanId.getInvalid(), spanData.get("parent_spanId"));
        Assertions.assertEquals(TraceId.getInvalid(), spanData.get("parent_traceId"));
        Assertions.assertFalse((Boolean) spanData.get("parent_valid"));
        Assertions.assertFalse((Boolean) spanData.get("parent_remote"));

        Assertions.assertEquals("GET", spanData.get("attr_http.method"));
        Assertions.assertEquals("1.1", spanData.get("attr_http.flavor"));
        Assertions.assertEquals("/direct", spanData.get("attr_http.target"));
        Assertions.assertEquals(directUrl.getAuthority(), spanData.get("attr_http.host"));
        Assertions.assertEquals("http", spanData.get("attr_http.scheme"));
        //TODO Waiting on a templatized route to be available
        //        Assertions.assertEquals("/direct", spanData.get("attr_http.route"));
        Assertions.assertEquals("200", spanData.get("attr_http.status_code"));
        Assertions.assertNotNull(spanData.get("attr_http.client_ip"));
        Assertions.assertNotNull(spanData.get("attr_http.user_agent"));
    }

    @Test
    void testChainedResourceTracing() {
        resetExporter();

        given()
                .contentType("application/json")
                .when().get("/chained")
                .then()
                .statusCode(200)
                .body("message", equalTo("Chained trace"));

        Awaitility.await().atMost(Duration.ofMinutes(2)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.get("spanId"));

        verifyResource(spanData);

        Assertions.assertEquals("chained", spanData.get("name"));
        Assertions.assertEquals(SpanKind.SERVER.toString(), spanData.get("kind"));
        Assertions.assertTrue((Boolean) spanData.get("ended"));

        Assertions.assertEquals(SpanId.getInvalid(), spanData.get("parent_spanId"));
        Assertions.assertEquals(TraceId.getInvalid(), spanData.get("parent_traceId"));
        Assertions.assertFalse((Boolean) spanData.get("parent_valid"));
        Assertions.assertFalse((Boolean) spanData.get("parent_remote"));

        Assertions.assertEquals("GET", spanData.get("attr_http.method"));
        Assertions.assertEquals("1.1", spanData.get("attr_http.flavor"));
        Assertions.assertEquals("/chained", spanData.get("attr_http.target"));
        Assertions.assertEquals(chainedUrl.getAuthority(), spanData.get("attr_http.host"));
        Assertions.assertEquals("http", spanData.get("attr_http.scheme"));
        //TODO Waiting on a templatized route to be available
        //        Assertions.assertEquals("/chained", spanData.get("attr_http.route"));
        Assertions.assertEquals("200", spanData.get("attr_http.status_code"));
        Assertions.assertNotNull(spanData.get("attr_http.client_ip"));
        Assertions.assertNotNull(spanData.get("attr_http.user_agent"));

        //TODO Update this when we support internal methods being traced
    }

    @Test
    void testTracingWithParentHeaders() {
        buildGlobalTelemetryInstance();
        resetExporter();

        Span parentSpan = GlobalOpenTelemetry.getTracer("io.quarkus.opentelemetry")
                .spanBuilder("testTracingWithParentHeaders")
                .setNoParent()
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();

        Context parentContext = Context.root().with(parentSpan);

        RequestSpecification requestSpec = given().contentType("application/json");

        // Inject Tracer header into REST call
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                .inject(parentContext, requestSpec, SETTER);

        requestSpec
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        Awaitility.await().atMost(Duration.ofMinutes(2)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.get("spanId"));

        verifyResource(spanData);

        Assertions.assertEquals("direct", spanData.get("name"));
        Assertions.assertEquals(SpanKind.SERVER.toString(), spanData.get("kind"));
        Assertions.assertTrue((Boolean) spanData.get("ended"));

        Assertions.assertEquals(parentSpan.getSpanContext().getSpanId(), spanData.get("parent_spanId"));
        Assertions.assertEquals(parentSpan.getSpanContext().getTraceId(), spanData.get("parent_traceId"));
        Assertions.assertTrue((Boolean) spanData.get("parent_remote"));
        Assertions.assertTrue((Boolean) spanData.get("parent_valid"));

        Assertions.assertEquals("GET", spanData.get("attr_http.method"));
        Assertions.assertEquals("1.1", spanData.get("attr_http.flavor"));
        Assertions.assertEquals("/direct", spanData.get("attr_http.target"));
        Assertions.assertEquals(directUrl.getAuthority(), spanData.get("attr_http.host"));
        Assertions.assertEquals("http", spanData.get("attr_http.scheme"));
        //TODO Waiting on a templatized route to be available
        //        Assertions.assertEquals("/direct", spanData.get("attr_http.route"));
        Assertions.assertEquals("200", spanData.get("attr_http.status_code"));
        Assertions.assertNotNull(spanData.get("attr_http.client_ip"));
        Assertions.assertNotNull(spanData.get("attr_http.user_agent"));
    }

    @Test
    void testDeepPathNaming() {
        resetExporter();

        given()
                .contentType("application/json")
                .when().get("/deep/path")
                .then()
                .statusCode(200)
                .body("message", equalTo("Deep url path"));

        Awaitility.await().atMost(Duration.ofMinutes(2)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.get("spanId"));

        verifyResource(spanData);

        Assertions.assertEquals("deep/path", spanData.get("name"));
        Assertions.assertEquals(SpanKind.SERVER.toString(), spanData.get("kind"));
        Assertions.assertTrue((Boolean) spanData.get("ended"));

        Assertions.assertEquals(SpanId.getInvalid(), spanData.get("parent_spanId"));
        Assertions.assertEquals(TraceId.getInvalid(), spanData.get("parent_traceId"));
        Assertions.assertFalse((Boolean) spanData.get("parent_valid"));
        Assertions.assertFalse((Boolean) spanData.get("parent_remote"));

        Assertions.assertEquals("GET", spanData.get("attr_http.method"));
        Assertions.assertEquals("1.1", spanData.get("attr_http.flavor"));
        Assertions.assertEquals("/deep/path", spanData.get("attr_http.target"));
        Assertions.assertEquals(deepPathUrl.getAuthority(), spanData.get("attr_http.host"));
        Assertions.assertEquals("http", spanData.get("attr_http.scheme"));
        //TODO Waiting on a templatized route to be available
        //        Assertions.assertEquals("/deep/path", spanData.get("attr_http.route"));
        Assertions.assertEquals("200", spanData.get("attr_http.status_code"));
        Assertions.assertNotNull(spanData.get("attr_http.client_ip"));
        Assertions.assertNotNull(spanData.get("attr_http.user_agent"));
    }

    @Test
    void testPathParameter() {
        resetExporter();

        given()
                .contentType("application/json")
                .when().get("/param/12345")
                .then()
                .statusCode(200)
                .body("message", equalTo("ParameterId: 12345"));

        Awaitility.await().atMost(Duration.ofMinutes(2)).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        Assertions.assertNotNull(spanData);
        Assertions.assertNotNull(spanData.get("spanId"));

        verifyResource(spanData);

        //TODO Need templatized route to verify span name is "param/{paramId}"
        Assertions.assertEquals("param/12345", spanData.get("name"));
        Assertions.assertEquals(SpanKind.SERVER.toString(), spanData.get("kind"));
        Assertions.assertTrue((Boolean) spanData.get("ended"));

        Assertions.assertEquals(SpanId.getInvalid(), spanData.get("parent_spanId"));
        Assertions.assertEquals(TraceId.getInvalid(), spanData.get("parent_traceId"));
        Assertions.assertFalse((Boolean) spanData.get("parent_valid"));
        Assertions.assertFalse((Boolean) spanData.get("parent_remote"));

        Assertions.assertEquals("GET", spanData.get("attr_http.method"));
        Assertions.assertEquals("1.1", spanData.get("attr_http.flavor"));
        Assertions.assertEquals("/param/12345", spanData.get("attr_http.target"));
        Assertions.assertEquals(pathParamUrl.getAuthority(), spanData.get("attr_http.host"));
        Assertions.assertEquals("http", spanData.get("attr_http.scheme"));
        //TODO Waiting on a templatized route to be available
        //        Assertions.assertEquals("/param/{paramId}", spanData.get("attr_http.route"));
        Assertions.assertEquals("200", spanData.get("attr_http.status_code"));
        Assertions.assertNotNull(spanData.get("attr_http.client_ip"));
        Assertions.assertNotNull(spanData.get("attr_http.user_agent"));
    }

    private void verifyResource(Map<String, Object> spanData) {
        Assertions.assertEquals("opentelemetry-integration-test", spanData.get("resource_service.name"));
        Assertions.assertEquals("999-SNAPSHOT", spanData.get("resource_service.version"));
        Assertions.assertEquals("java", spanData.get("resource_telemetry.sdk.language"));
        Assertions.assertEquals("opentelemetry", spanData.get("resource_telemetry.sdk.name"));
        Assertions.assertNotNull(spanData.get("resource_telemetry.sdk.version"));
    }

    protected void buildGlobalTelemetryInstance() {
        // Do nothing in JVM mode
    }

    private static final TextMapSetter<RequestSpecification> SETTER = new TextMapSetter<RequestSpecification>() {
        @Override
        public void set(RequestSpecification carrier, String key, String value) {
            if (carrier != null) {
                carrier.header(key, value);
            }
        }
    };
}
