package io.quarkus.it.opentelemetry;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.SemanticAttributes;
import io.quarkus.it.opentelemetry.util.SocketClient;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class TracingTest {
    @TestHTTPResource("direct")
    URL directUrl;
    @TestHTTPResource("chained")
    URL chainedUrl;
    @TestHTTPResource("deep/path")
    URL deepPathUrl;
    @TestHTTPResource("param")
    URL pathParamUrl;

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
    void testEmptyClientPath() {
        given()
                .contentType("application/json")
                .when().get("/nopath")
                .then()
                .statusCode(200)
                .body("message", equalTo("No path trace"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals(SERVER.toString(), server.get("kind"));
        verifyResource(server);
        assertEquals("GET /nopath", server.get("name"));
        assertEquals(SERVER.toString(), server.get("kind"));
        assertTrue((Boolean) server.get("ended"));
        assertEquals(SpanId.getInvalid(), server.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), server.get("parent_traceId"));
        assertFalse((Boolean) server.get("parent_valid"));
        assertFalse((Boolean) server.get("parent_remote"));
        assertEquals("GET", server.get("attr_http.request.method"));
        assertEquals("/nopath", server.get("attr_url.path"));
        assertEquals(pathParamUrl.getHost(), server.get("attr_server.address"));
        assertEquals(pathParamUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", server.get("attr_url.scheme"));
        assertEquals("/nopath", server.get("attr_http.route"));
        assertEquals("200", server.get("attr_http.response.status_code"));
        assertNotNull(server.get("attr_client.address"));
        assertNotNull(server.get("attr_user_agent.original"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, server.get("spanId"));
        assertEquals(CLIENT.toString(), client.get("kind"));
        verifyResource(client);
        assertEquals("GET /", client.get("name"));
        assertEquals(SpanKind.CLIENT.toString(), client.get("kind"));
        assertTrue((Boolean) client.get("ended"));
        assertTrue((Boolean) client.get("parent_valid"));
        assertFalse((Boolean) client.get("parent_remote"));
        assertEquals("GET", client.get("attr_http.request.method"));
        assertEquals("http://localhost:8081", client.get("attr_url.full"));
        assertEquals("200", client.get("attr_http.response.status_code"));
        assertEquals(client.get("parentSpanId"), server.get("spanId"));

        Map<String, Object> clientServer = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals(SERVER.toString(), clientServer.get("kind"));
        verifyResource(clientServer);
        assertEquals("GET /", clientServer.get("name"));
        assertEquals(SERVER.toString(), clientServer.get("kind"));
        assertTrue((Boolean) clientServer.get("ended"));
        assertTrue((Boolean) clientServer.get("parent_valid"));
        assertTrue((Boolean) clientServer.get("parent_remote"));
        assertEquals("GET", clientServer.get("attr_http.request.method"));
        assertEquals("/", clientServer.get("attr_url.path"));
        assertEquals(pathParamUrl.getHost(), server.get("attr_server.address"));
        assertEquals(pathParamUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", clientServer.get("attr_url.scheme"));
        assertEquals("/", clientServer.get("attr_http.route"));
        assertEquals("200", clientServer.get("attr_http.response.status_code"));
        assertNotNull(clientServer.get("attr_client.address"));
        assertNotNull(clientServer.get("attr_user_agent.original"));
        assertEquals(clientServer.get("parentSpanId"), client.get("spanId"));
    }

    @Test
    void testSlashClientPath() {
        given()
                .contentType("application/json")
                .when().get("/slashpath")
                .then()
                .statusCode(200)
                .body("message", equalTo("No path trace"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals(SERVER.toString(), server.get("kind"));
        verifyResource(server);
        assertEquals("GET /slashpath", server.get("name"));
        assertEquals(SERVER.toString(), server.get("kind"));
        assertTrue((Boolean) server.get("ended"));
        assertEquals(SpanId.getInvalid(), server.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), server.get("parent_traceId"));
        assertFalse((Boolean) server.get("parent_valid"));
        assertFalse((Boolean) server.get("parent_remote"));
        assertEquals("GET", server.get("attr_http.request.method"));
        assertEquals("/slashpath", server.get("attr_url.path"));
        assertEquals(pathParamUrl.getHost(), server.get("attr_server.address"));
        assertEquals(pathParamUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", server.get("attr_url.scheme"));
        assertEquals("/slashpath", server.get("attr_http.route"));
        assertEquals("200", server.get("attr_http.response.status_code"));
        assertNotNull(server.get("attr_client.address"));
        assertNotNull(server.get("attr_user_agent.original"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, server.get("spanId"));
        assertEquals(CLIENT.toString(), client.get("kind"));
        assertEquals("GET /", client.get("name"));
        assertEquals(SpanKind.CLIENT.toString(), client.get("kind"));
        assertTrue((Boolean) client.get("ended"));
        assertTrue((Boolean) client.get("parent_valid"));
        assertFalse((Boolean) client.get("parent_remote"));
        assertEquals("GET", client.get("attr_http.request.method"));
        assertEquals("http://localhost:8081/", client.get("attr_url.full"));
        assertEquals("200", client.get("attr_http.response.status_code"));
        assertEquals(client.get("parentSpanId"), server.get("spanId"));

        Map<String, Object> clientServer = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals(SERVER.toString(), clientServer.get("kind"));
        verifyResource(clientServer);
        assertEquals("GET /", clientServer.get("name"));
        assertEquals(SERVER.toString(), clientServer.get("kind"));
        assertTrue((Boolean) clientServer.get("ended"));
        assertTrue((Boolean) clientServer.get("parent_valid"));
        assertTrue((Boolean) clientServer.get("parent_remote"));
        assertEquals("GET", clientServer.get("attr_http.request.method"));
        assertEquals("/", clientServer.get("attr_url.path"));
        assertEquals(pathParamUrl.getHost(), server.get("attr_server.address"));
        assertEquals(pathParamUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", clientServer.get("attr_url.scheme"));
        assertEquals("/", clientServer.get("attr_http.route"));
        assertEquals("200", clientServer.get("attr_http.response.status_code"));
        assertNotNull(clientServer.get("attr_client.address"));
        assertNotNull(clientServer.get("attr_user_agent.original"));
        assertEquals(clientServer.get("parentSpanId"), client.get("spanId"));
    }

    @Test
    void testBaggagePath() {
        given()
                .contentType("application/json")
                .when().get("/slashpath-baggage")
                .then()
                .statusCode(200)
                .body("message", equalTo("baggage-value"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals(SERVER.toString(), server.get("kind"));
        verifyResource(server);
        assertEquals("GET /slashpath-baggage", server.get("name"));
        assertEquals(SpanId.getInvalid(), server.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), server.get("parent_traceId"));
        assertFalse((Boolean) server.get("parent_valid"));
        assertFalse((Boolean) server.get("parent_remote"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, server.get("spanId"));
        assertEquals(CLIENT.toString(), client.get("kind"));
        assertEquals("GET /from-baggage", client.get("name"));
        assertEquals("http://localhost:8081/from-baggage", client.get("attr_url.full"));
        assertEquals("200", client.get("attr_http.response.status_code"));
        assertEquals(client.get("parentSpanId"), server.get("spanId"));

        Map<String, Object> clientServer = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals(SERVER.toString(), clientServer.get("kind"));
        verifyResource(clientServer);
        assertEquals("GET /from-baggage", clientServer.get("name"));
        assertEquals(clientServer.get("parentSpanId"), client.get("spanId"));
    }

    @Test
    void testChainedResourceTracing() {
        given()
                .contentType("application/json")
                .when().get("/chained")
                .then()
                .statusCode(200)
                .body("message", equalTo("Chained trace"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 2);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(2, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals(SERVER.toString(), server.get("kind"));
        verifyResource(server);
        assertEquals("GET /chained", server.get("name"));
        assertEquals(SERVER.toString(), server.get("kind"));
        assertTrue((Boolean) server.get("ended"));
        assertEquals(SpanId.getInvalid(), server.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), server.get("parent_traceId"));
        assertFalse((Boolean) server.get("parent_valid"));
        assertFalse((Boolean) server.get("parent_remote"));
        assertEquals("GET", server.get("attr_http.request.method"));
        assertEquals("/chained", server.get("attr_url.path"));
        assertEquals(deepPathUrl.getHost(), server.get("attr_server.address"));
        assertEquals(deepPathUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", server.get("attr_url.scheme"));
        assertEquals("200", server.get("attr_http.response.status_code"));
        assertNotNull(server.get("attr_client.address"));
        assertNotNull(server.get("attr_user_agent.original"));

        // CDI call
        Map<String, Object> cdi = getSpanByKindAndParentId(spans, INTERNAL, server.get("spanId"));
        assertEquals("TracedService.call", cdi.get("name"));
        assertEquals(SpanKind.INTERNAL.toString(), cdi.get("kind"));
        assertEquals(server.get("spanId"), cdi.get("parent_spanId"));
    }

    @Test
    void testTracingWithParentHeaders() {
        buildGlobalTelemetryInstance();
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

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        assertNotNull(spanData);
        assertNotNull(spanData.get("spanId"));

        verifyResource(spanData);

        assertEquals("GET /direct", spanData.get("name"));
        assertEquals(SERVER.toString(), spanData.get("kind"));
        assertTrue((Boolean) spanData.get("ended"));

        assertEquals(parentSpan.getSpanContext().getSpanId(), spanData.get("parent_spanId"));
        assertEquals(parentSpan.getSpanContext().getTraceId(), spanData.get("parent_traceId"));
        assertTrue((Boolean) spanData.get("parent_remote"));
        assertTrue((Boolean) spanData.get("parent_valid"));

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
    void testDeepPathNaming() {
        given()
                .contentType("application/json")
                .when().get("/deep/path")
                .then()
                .statusCode(200)
                .body("message", equalTo("Deep url path"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        assertNotNull(spanData);
        assertNotNull(spanData.get("spanId"));

        verifyResource(spanData);

        assertEquals("GET /deep/path", spanData.get("name"));
        assertEquals(SERVER.toString(), spanData.get("kind"));
        assertTrue((Boolean) spanData.get("ended"));

        assertEquals(SpanId.getInvalid(), spanData.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), spanData.get("parent_traceId"));
        assertFalse((Boolean) spanData.get("parent_valid"));
        assertFalse((Boolean) spanData.get("parent_remote"));

        assertEquals("GET", spanData.get("attr_http.request.method"));
        assertEquals("/deep/path", spanData.get("attr_url.path"));
        assertEquals(deepPathUrl.getHost(), spanData.get("attr_server.address"));
        assertEquals(deepPathUrl.getPort(), Integer.valueOf((String) spanData.get("attr_server.port")));
        assertEquals("http", spanData.get("attr_url.scheme"));
        assertEquals("200", spanData.get("attr_http.response.status_code"));
        assertNotNull(spanData.get("attr_client.address"));
        assertNotNull(spanData.get("attr_user_agent.original"));
    }

    @Test
    void testPathParameter() {
        given()
                .contentType("application/json")
                .when().get("/param/12345")
                .then()
                .statusCode(200)
                .body("message", equalTo("ParameterId: 12345"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        assertNotNull(spanData);
        assertNotNull(spanData.get("spanId"));

        verifyResource(spanData);

        assertEquals("GET /param/{paramId}", spanData.get("name"));
        assertEquals(SERVER.toString(), spanData.get("kind"));
        assertTrue((Boolean) spanData.get("ended"));

        assertEquals(SpanId.getInvalid(), spanData.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), spanData.get("parent_traceId"));
        assertFalse((Boolean) spanData.get("parent_valid"));
        assertFalse((Boolean) spanData.get("parent_remote"));

        assertEquals("GET", spanData.get("attr_http.request.method"));
        assertEquals("/param/12345", spanData.get("attr_url.path"));
        assertEquals(pathParamUrl.getHost(), spanData.get("attr_server.address"));
        assertEquals(pathParamUrl.getPort(), Integer.valueOf((String) spanData.get("attr_server.port")));
        assertEquals("http", spanData.get("attr_url.scheme"));
        assertEquals("/param/{paramId}", spanData.get("attr_http.route"));
        assertEquals("200", spanData.get("attr_http.response.status_code"));
        assertNotNull(spanData.get("attr_client.address"));
        assertNotNull(spanData.get("attr_user_agent.original"));
    }

    @Test
    void testClientTracing() {
        given()
                .when().get("/client/ping/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals(SERVER.toString(), server.get("kind"));
        verifyResource(server);
        assertEquals("GET /client/ping/{message}", server.get("name"));
        assertEquals(SERVER.toString(), server.get("kind"));
        assertTrue((Boolean) server.get("ended"));
        assertEquals(SpanId.getInvalid(), server.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), server.get("parent_traceId"));
        assertFalse((Boolean) server.get("parent_valid"));
        assertFalse((Boolean) server.get("parent_remote"));
        assertEquals("GET", server.get("attr_http.request.method"));
        assertEquals("/client/ping/one", server.get("attr_url.path"));
        assertEquals(pathParamUrl.getHost(), server.get("attr_server.address"));
        assertEquals(pathParamUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", server.get("attr_url.scheme"));
        assertEquals("/client/ping/{message}", server.get("attr_http.route"));
        assertEquals("200", server.get("attr_http.response.status_code"));
        assertNotNull(server.get("attr_client.address"));
        assertNotNull(server.get("attr_user_agent.original"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, server.get("spanId"));
        assertEquals("GET /client/pong/{message}", client.get("name"));
        assertEquals(SpanKind.CLIENT.toString(), client.get("kind"));
        assertTrue((Boolean) client.get("ended"));
        assertTrue((Boolean) client.get("parent_valid"));
        assertFalse((Boolean) client.get("parent_remote"));
        assertEquals("GET", client.get("attr_http.request.method"));
        assertEquals("http://localhost:8081/client/pong/one", client.get("attr_url.full"));
        assertEquals("200", client.get("attr_http.response.status_code"));

        Map<String, Object> clientServer = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals(SERVER.toString(), clientServer.get("kind"));
        verifyResource(clientServer);
        assertEquals("GET /client/pong/{message}", clientServer.get("name"));
        assertEquals(SERVER.toString(), clientServer.get("kind"));
        assertTrue((Boolean) clientServer.get("ended"));
        assertTrue((Boolean) clientServer.get("parent_valid"));
        assertTrue((Boolean) clientServer.get("parent_remote"));
        assertEquals("GET", clientServer.get("attr_http.request.method"));
        assertEquals("/client/pong/one", clientServer.get("attr_url.path"));
        assertEquals(pathParamUrl.getHost(), server.get("attr_server.address"));
        assertEquals(pathParamUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", clientServer.get("attr_url.scheme"));
        assertEquals("/client/pong/{message}", clientServer.get("attr_http.route"));
        assertEquals("200", clientServer.get("attr_http.response.status_code"));
        assertNotNull(clientServer.get("attr_client.address"));
        assertNotNull(clientServer.get("attr_user_agent.original"));
        assertEquals(clientServer.get("parentSpanId"), client.get("spanId"));
    }

    @Test
    void testAsyncClientTracing() {
        given()
                .when().get("/client/async-ping/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals(SERVER.toString(), server.get("kind"));
        verifyResource(server);
        assertEquals("GET /client/async-ping/{message}", server.get("name"));
        assertEquals(SERVER.toString(), server.get("kind"));
        assertTrue((Boolean) server.get("ended"));
        assertEquals(SpanId.getInvalid(), server.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), server.get("parent_traceId"));
        assertFalse((Boolean) server.get("parent_valid"));
        assertFalse((Boolean) server.get("parent_remote"));
        assertEquals("GET", server.get("attr_http.request.method"));
        assertEquals("/client/async-ping/one", server.get("attr_url.path"));
        assertEquals(pathParamUrl.getHost(), server.get("attr_server.address"));
        assertEquals(pathParamUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", server.get("attr_url.scheme"));
        assertEquals("/client/async-ping/{message}", server.get("attr_http.route"));
        assertEquals("200", server.get("attr_http.response.status_code"));
        assertNotNull(server.get("attr_client.address"));
        assertNotNull(server.get("attr_user_agent.original"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, server.get("spanId"));
        assertEquals("GET /client/pong/{message}", client.get("name"));
        assertEquals(SpanKind.CLIENT.toString(), client.get("kind"));
        assertTrue((Boolean) client.get("ended"));
        assertTrue((Boolean) client.get("parent_valid"));
        assertFalse((Boolean) client.get("parent_remote"));
        assertEquals("GET", client.get("attr_http.request.method"));
        assertEquals("http://localhost:8081/client/pong/one", client.get("attr_url.full"));
        assertEquals("200", client.get("attr_http.response.status_code"));

        Map<String, Object> clientServer = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals(SERVER.toString(), clientServer.get("kind"));
        verifyResource(clientServer);
        assertEquals("GET /client/pong/{message}", clientServer.get("name"));
        assertEquals(SERVER.toString(), clientServer.get("kind"));
        assertTrue((Boolean) clientServer.get("ended"));
        assertTrue((Boolean) clientServer.get("parent_valid"));
        assertTrue((Boolean) clientServer.get("parent_remote"));
        assertEquals("GET", clientServer.get("attr_http.request.method"));
        assertEquals("/client/pong/one", clientServer.get("attr_url.path"));
        assertEquals(pathParamUrl.getHost(), server.get("attr_server.address"));
        assertEquals(pathParamUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", clientServer.get("attr_url.scheme"));
        assertEquals("/client/pong/{message}", clientServer.get("attr_http.route"));
        assertEquals("200", clientServer.get("attr_http.response.status_code"));
        assertNotNull(clientServer.get("attr_client.address"));
        assertNotNull(clientServer.get("attr_user_agent.original"));
    }

    @Test
    void testClientTracingWithInterceptor() {
        given()
                .when().get("/client/pong-intercept/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 4);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(4, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals(SERVER.toString(), server.get("kind"));
        verifyResource(server);
        assertEquals("GET /client/pong-intercept/{message}", server.get("name"));
        assertEquals(SERVER.toString(), server.get("kind"));
        assertTrue((Boolean) server.get("ended"));
        assertEquals(SpanId.getInvalid(), server.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), server.get("parent_traceId"));
        assertFalse((Boolean) server.get("parent_valid"));
        assertFalse((Boolean) server.get("parent_remote"));
        assertEquals("GET", server.get("attr_http.request.method"));
        assertEquals("/client/pong-intercept/one", server.get("attr_url.path"));
        assertEquals(pathParamUrl.getHost(), server.get("attr_server.address"));
        assertEquals(pathParamUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", server.get("attr_url.scheme"));
        assertEquals("/client/pong-intercept/{message}", server.get("attr_http.route"));
        assertEquals("200", server.get("attr_http.response.status_code"));
        assertNotNull(server.get("attr_client.address"));
        assertNotNull(server.get("attr_user_agent.original"));

        Map<String, Object> fromInterceptor = getSpanByKindAndParentId(spans, INTERNAL, server.get("spanId"));
        assertEquals("PingPongRestClient.pingpongIntercept", fromInterceptor.get("name"));
        assertEquals(INTERNAL.toString(), fromInterceptor.get("kind"));
        assertTrue((Boolean) fromInterceptor.get("ended"));
        assertTrue((Boolean) fromInterceptor.get("parent_valid"));
        assertFalse((Boolean) fromInterceptor.get("parent_remote"));
        assertNull(fromInterceptor.get("attr_http.request.method"));
        assertNull(fromInterceptor.get("attr_http.response.status_code"));
        assertEquals("one", fromInterceptor.get("attr_message"));

        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, fromInterceptor.get("spanId"));
        assertEquals("GET /client/pong/{message}", client.get("name"));
        assertEquals(SpanKind.CLIENT.toString(), client.get("kind"));
        assertTrue((Boolean) client.get("ended"));
        assertTrue((Boolean) client.get("parent_valid"));
        assertFalse((Boolean) client.get("parent_remote"));
        assertEquals("GET", client.get("attr_http.request.method"));
        assertEquals("http://localhost:8081/client/pong/one", client.get("attr_url.full"));
        assertEquals("200", client.get("attr_http.response.status_code"));

        Map<String, Object> clientServer = getSpanByKindAndParentId(spans, SERVER, client.get("spanId"));
        assertEquals(SERVER.toString(), clientServer.get("kind"));
        verifyResource(clientServer);
        assertEquals("GET /client/pong/{message}", clientServer.get("name"));
        assertEquals(SERVER.toString(), clientServer.get("kind"));
        assertTrue((Boolean) clientServer.get("ended"));
        assertTrue((Boolean) clientServer.get("parent_valid"));
        assertTrue((Boolean) clientServer.get("parent_remote"));
        assertEquals("GET", clientServer.get("attr_http.request.method"));
        assertEquals("/client/pong/one", clientServer.get("attr_url.path"));
        assertEquals(pathParamUrl.getHost(), server.get("attr_server.address"));
        assertEquals(pathParamUrl.getPort(), Integer.valueOf((String) server.get("attr_server.port")));
        assertEquals("http", clientServer.get("attr_url.scheme"));
        assertEquals("/client/pong/{message}", clientServer.get("attr_http.route"));
        assertEquals("200", clientServer.get("attr_http.response.status_code"));
        assertNotNull(clientServer.get("attr_client.address"));
        assertNotNull(clientServer.get("attr_user_agent.original"));
        assertEquals(clientServer.get("parentSpanId"), client.get("spanId"));
    }

    @Test
    void testTemplatedPathOnClass() {
        given()
                .contentType("application/json")
                .when().get("/template/path/something")
                .then()
                .statusCode(200)
                .body(containsString("Received: something"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
        Map<String, Object> spanData = getSpans().get(0);
        assertNotNull(spanData);
        assertNotNull(spanData.get("spanId"));

        verifyResource(spanData);

        assertEquals("GET /template/path/{value}", spanData.get("name"));
        assertEquals(SERVER.toString(), spanData.get("kind"));
        assertTrue((Boolean) spanData.get("ended"));

        assertEquals(SpanId.getInvalid(), spanData.get("parent_spanId"));
        assertEquals(TraceId.getInvalid(), spanData.get("parent_traceId"));
        assertFalse((Boolean) spanData.get("parent_valid"));
        assertFalse((Boolean) spanData.get("parent_remote"));

        assertEquals("GET", spanData.get("attr_http.request.method"));
        assertEquals("/template/path/something", spanData.get("attr_url.path"));
        assertEquals(deepPathUrl.getHost(), spanData.get("attr_server.address"));
        assertEquals(deepPathUrl.getPort(), Integer.valueOf((String) spanData.get("attr_server.port")));
        assertEquals("http", spanData.get("attr_url.scheme"));
        assertEquals("200", spanData.get("attr_http.response.status_code"));
        assertNotNull(spanData.get("attr_client.address"));
        assertNotNull(spanData.get("attr_user_agent.original"));
    }

    @Test
    void testCustomSpanNames() {
        given()
                .contentType("application/json")
                .when().get("/client/async-ping-named/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        Map<String, Object> client = getSpanByKindAndParentId(spans, CLIENT, server.get("spanId"));

        verifyResource(client);
        assertEquals("Async Ping", client.get("name"));
    }

    /**
     * From bug #26149
     * NPE was thrown when HTTP version was not supported with OpenTelemetry
     */
    @Test
    void testWrongHTTPVersion() {
        final int port = RestAssured.port;
        final String host = URI.create(RestAssured.baseURI).getHost();

        try (SocketClient sc = new SocketClient(host, port)) {
            assertEquals("HTTP/50.0 501 Not Implemented",
                    sc.sendMessage("GET /client/ping/1 HTTP/50.0\r\n\r\n"));
        } catch (IOException e) {
            fail("Not failing graciously. Got: " + e.getMessage());
        }
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 1);
    }

    /**
     * Test no End User attributes are added when the feature is disabled.
     */
    @Test
    public void testNoEndUserAttributes() {
        RestAssured
                .given()
                .auth().preemptive().basic("stuart", "writer")
                .get("/otel/enduser/roles-allowed-only-writer-role")
                .then()
                .statusCode(200)
                .body(Matchers.is("/roles-allowed-only-writer-role"));
        RestAssured
                .given()
                .auth().preemptive().basic("scott", "reader")
                .get("/otel/enduser/roles-allowed-only-writer-role")
                .then()
                .statusCode(403);
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() > 1);
        List<Map<String, Object>> spans = getSpans();
        Assertions.assertTrue(spans
                .stream()
                .flatMap(m -> m.entrySet().stream())
                .filter(e -> ("attr_" + SemanticAttributes.ENDUSER_ID.getKey()).equals(e.getKey())
                        || ("attr_" + SemanticAttributes.ENDUSER_ROLE.getKey()).equals(e.getKey()))
                .findAny().isEmpty());
    }

    @Test
    public void testSuppressAppUri() {
        RestAssured.given()
                .when().get("/suppress-app-uri")
                .then()
                .statusCode(200)
                .body("message", Matchers.is("Suppress me!"));

        // should throw because there are a configuration quarkus.otel.traces.suppress-app-uris=/suppress-app-uri
        assertThrows(ConditionTimeoutException.class, () -> {
            await().atMost(5, SECONDS).until(() -> !getSpans().isEmpty());
        });
    }

    private void verifyResource(Map<String, Object> spanData) {
        assertEquals("opentelemetry-integration-test", spanData.get("resource_service.name"));
        assertEquals("999-SNAPSHOT", spanData.get("resource_service.version"));
        assertEquals("java", spanData.get("resource_telemetry.sdk.language"));
        assertEquals("opentelemetry", spanData.get("resource_telemetry.sdk.name"));
        assertNotNull(spanData.get("resource_telemetry.sdk.version"));
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
