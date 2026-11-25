package io.quarkus.it.opentelemetry;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.ClientAttributes.CLIENT_ADDRESS;
import static io.opentelemetry.semconv.CodeAttributes.CODE_FUNCTION_NAME;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_INSTANCE_ID;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_LANGUAGE;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_NAME;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_VERSION;
import static io.opentelemetry.semconv.UrlAttributes.URL_FULL;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_SCHEME;
import static io.opentelemetry.semconv.UserAgentAttributes.USER_AGENT_ORIGINAL;
import static io.opentelemetry.semconv.incubating.EnduserIncubatingAttributes.ENDUSER_ID;
import static io.opentelemetry.semconv.incubating.UserIncubatingAttributes.USER_ROLES;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.arc.Arc;
import io.quarkus.it.opentelemetry.util.SocketClient;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class TracingTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

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

        SpanData spanData = spanExporter().awaitCount(1, Duration.ofMinutes(2)).single();
        assertNotNull(spanData.getSpanId());

        verifyResource(spanData);

        assertThat(spanData)
                .hasName("GET /direct")
                .hasKind(SERVER);
        assertTrue(spanData.hasEnded());

        assertEquals(SpanId.getInvalid(), spanData.getParentSpanContext().getSpanId());
        assertThat(spanData).hasNoParent();
        assertFalse(spanData.getParentSpanContext().isRemote());

        assertThat(spanData)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/direct")
                .hasAttribute(SERVER_ADDRESS, deepPathUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) deepPathUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(spanData.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(spanData.getAttributes().get(USER_AGENT_ORIGINAL));
    }

    @Test
    void testEmptyClientPath() {
        given()
                .contentType("application/json")
                .when().get("/nopath")
                .then()
                .statusCode(200)
                .body("message", equalTo("No path trace"));

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData server = spans.ofKind(SERVER).roots().single();
        verifyResource(server);
        assertThat(server).hasName("GET /nopath");
        assertTrue(server.hasEnded());
        assertEquals(SpanId.getInvalid(), server.getParentSpanContext().getSpanId());
        assertThat(server).hasNoParent();
        assertFalse(server.getParentSpanContext().isRemote());
        assertThat(server)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/nopath")
                .hasAttribute(SERVER_ADDRESS, pathParamUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) pathParamUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_ROUTE, "/nopath")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(server.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(server.getAttributes().get(USER_AGENT_ORIGINAL));

        SpanData client = spans.ofKind(CLIENT).childrenOf(server).single();
        verifyResource(client);
        assertThat(client).hasName("GET /");
        assertTrue(client.hasEnded());
        assertTrue(client.getParentSpanContext().isValid());
        assertFalse(client.getParentSpanContext().isRemote());
        assertThat(client)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_FULL, "http://localhost:8081/")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L)
                .hasParent(server);

        SpanData clientServer = spans.ofKind(SERVER).childrenOf(client).single();
        verifyResource(clientServer);
        assertThat(clientServer).hasName("GET /");
        assertTrue(clientServer.hasEnded());
        assertTrue(clientServer.getParentSpanContext().isValid());
        assertTrue(clientServer.getParentSpanContext().isRemote());
        assertThat(clientServer)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/");
        assertThat(server)
                .hasAttribute(SERVER_ADDRESS, pathParamUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) pathParamUrl.getPort());
        assertThat(clientServer)
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_ROUTE, "/")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(clientServer.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(clientServer.getAttributes().get(USER_AGENT_ORIGINAL));
        assertThat(clientServer).hasParent(client);
    }

    @Test
    void testSlashClientPath() {
        given()
                .contentType("application/json")
                .when().get("/slashpath")
                .then()
                .statusCode(200)
                .body("message", equalTo("No path trace"));

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData server = spans.ofKind(SERVER).roots().single();
        verifyResource(server);
        assertThat(server).hasName("GET /slashpath");
        assertTrue(server.hasEnded());
        assertEquals(SpanId.getInvalid(), server.getParentSpanContext().getSpanId());
        assertThat(server).hasNoParent();
        assertFalse(server.getParentSpanContext().isRemote());
        assertThat(server)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/slashpath")
                .hasAttribute(SERVER_ADDRESS, pathParamUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) pathParamUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_ROUTE, "/slashpath")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(server.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(server.getAttributes().get(USER_AGENT_ORIGINAL));

        SpanData client = spans.ofKind(CLIENT).childrenOf(server).single();
        assertThat(client).hasName("GET /");
        assertTrue(client.hasEnded());
        assertTrue(client.getParentSpanContext().isValid());
        assertFalse(client.getParentSpanContext().isRemote());
        assertThat(client)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_FULL, "http://localhost:8081/")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L)
                .hasParent(server);

        SpanData clientServer = spans.ofKind(SERVER).childrenOf(client).single();
        verifyResource(clientServer);
        assertThat(clientServer).hasName("GET /");
        assertTrue(clientServer.hasEnded());
        assertTrue(clientServer.getParentSpanContext().isValid());
        assertTrue(clientServer.getParentSpanContext().isRemote());
        assertThat(clientServer)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/");
        assertThat(server)
                .hasAttribute(SERVER_ADDRESS, pathParamUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) pathParamUrl.getPort());
        assertThat(clientServer)
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_ROUTE, "/")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(clientServer.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(clientServer.getAttributes().get(USER_AGENT_ORIGINAL));
        assertThat(clientServer).hasParent(client);
    }

    @Test
    void testBaggagePath() {
        given()
                .contentType("application/json")
                .when().get("/slashpath-baggage")
                .then()
                .statusCode(200)
                .body("message", equalTo("baggage-value"));

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData server = spans.ofKind(SERVER).roots().single();
        verifyResource(server);
        assertThat(server).hasName("GET /slashpath-baggage");
        assertEquals(SpanId.getInvalid(), server.getParentSpanContext().getSpanId());
        assertThat(server).hasNoParent();
        assertFalse(server.getParentSpanContext().isRemote());

        SpanData client = spans.ofKind(CLIENT).childrenOf(server).single();
        assertThat(client)
                .hasName("GET /from-baggage")
                .hasAttribute(URL_FULL, "http://localhost:8081/from-baggage")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L)
                .hasParent(server);

        SpanData clientServer = spans.ofKind(SERVER).childrenOf(client).single();
        verifyResource(clientServer);
        assertThat(clientServer)
                .hasName("GET /from-baggage")
                .hasParent(client);
    }

    @Test
    void testChainedResourceTracing() {
        given()
                .contentType("application/json")
                .when().get("/chained")
                .then()
                .statusCode(200)
                .body("message", equalTo("Chained trace"));

        SpanDataList spans = spanExporter().awaitCount(2, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData server = spans.ofKind(SERVER).roots().single();
        verifyResource(server);
        assertThat(server).hasName("GET /chained");
        assertTrue(server.hasEnded());
        assertEquals(SpanId.getInvalid(), server.getParentSpanContext().getSpanId());
        assertThat(server).hasNoParent();
        assertFalse(server.getParentSpanContext().isRemote());
        assertThat(server)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/chained")
                .hasAttribute(SERVER_ADDRESS, deepPathUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) deepPathUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(server.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(server.getAttributes().get(USER_AGENT_ORIGINAL));

        // CDI call
        SpanData cdi = spans.ofKind(INTERNAL).childrenOf(server).single();
        assertThat(cdi)
                .hasName("TracedService.call")
                .hasParent(server)
                .hasAttribute(CODE_FUNCTION_NAME, "io.quarkus.it.opentelemetry.TracedService.call");
    }

    @Test
    void testTracingWithParentHeaders() {
        Span parentSpan = createParentSpan();

        Context parentContext = Context.root().with(parentSpan);

        RequestSpecification requestSpec = given().contentType("application/json");

        // Inject Tracer header into REST call
        parentSpanPropagator().inject(parentContext, requestSpec, SETTER);

        requestSpec
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        SpanData spanData = spanExporter().awaitCount(1, Duration.ofSeconds(10)).single();
        assertNotNull(spanData.getSpanId());

        verifyResource(spanData);

        assertThat(spanData)
                .hasName("GET /direct")
                .hasKind(SERVER);
        assertTrue(spanData.hasEnded());

        assertEquals(parentSpan.getSpanContext().getSpanId(), spanData.getParentSpanContext().getSpanId());
        assertEquals(parentSpan.getSpanContext().getTraceId(), spanData.getTraceId());
        assertEquals(parentSpan.getSpanContext().getTraceId(), spanData.getParentSpanContext().getTraceId());
        assertTrue(spanData.getParentSpanContext().isRemote());
        assertTrue(spanData.getParentSpanContext().isValid());

        assertThat(spanData)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/direct")
                .hasAttribute(SERVER_ADDRESS, deepPathUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) deepPathUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(spanData.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(spanData.getAttributes().get(USER_AGENT_ORIGINAL));
    }

    @Test
    void testDeepPathNaming() {
        given()
                .contentType("application/json")
                .when().get("/deep/path")
                .then()
                .statusCode(200)
                .body("message", equalTo("Deep url path"));

        SpanData spanData = spanExporter().awaitCount(1, Duration.ofSeconds(10)).single();
        assertNotNull(spanData.getSpanId());

        verifyResource(spanData);

        assertThat(spanData)
                .hasName("GET /deep/path")
                .hasKind(SERVER);
        assertTrue(spanData.hasEnded());

        assertEquals(SpanId.getInvalid(), spanData.getParentSpanContext().getSpanId());
        assertThat(spanData).hasNoParent();
        assertFalse(spanData.getParentSpanContext().isRemote());

        assertThat(spanData)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/deep/path")
                .hasAttribute(SERVER_ADDRESS, deepPathUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) deepPathUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(spanData.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(spanData.getAttributes().get(USER_AGENT_ORIGINAL));
    }

    @Test
    void testPathParameter() {
        given()
                .contentType("application/json")
                .when().get("/param/12345")
                .then()
                .statusCode(200)
                .body("message", equalTo("ParameterId: 12345"));

        SpanData spanData = spanExporter().awaitCount(1, Duration.ofSeconds(10)).single();
        assertNotNull(spanData.getSpanId());

        verifyResource(spanData);

        assertThat(spanData)
                .hasName("GET /param/{paramId}")
                .hasKind(SERVER);
        assertTrue(spanData.hasEnded());

        assertEquals(SpanId.getInvalid(), spanData.getParentSpanContext().getSpanId());
        assertThat(spanData).hasNoParent();
        assertFalse(spanData.getParentSpanContext().isRemote());

        assertThat(spanData)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/param/12345")
                .hasAttribute(SERVER_ADDRESS, pathParamUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) pathParamUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_ROUTE, "/param/{paramId}")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(spanData.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(spanData.getAttributes().get(USER_AGENT_ORIGINAL));
    }

    @Test
    void testClientTracing() {
        given()
                .when().get("/client/ping/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData server = spans.ofKind(SERVER).roots().single();
        verifyResource(server);
        assertThat(server).hasName("GET /client/ping/{message}");
        assertTrue(server.hasEnded());
        assertEquals(SpanId.getInvalid(), server.getParentSpanContext().getSpanId());
        assertThat(server).hasNoParent();
        assertFalse(server.getParentSpanContext().isRemote());
        assertThat(server)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/client/ping/one")
                .hasAttribute(SERVER_ADDRESS, pathParamUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) pathParamUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_ROUTE, "/client/ping/{message}")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(server.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(server.getAttributes().get(USER_AGENT_ORIGINAL));

        SpanData client = spans.ofKind(CLIENT).childrenOf(server).single();
        assertThat(client).hasName("GET /client/pong/{message}");
        assertTrue(client.hasEnded());
        assertTrue(client.getParentSpanContext().isValid());
        assertFalse(client.getParentSpanContext().isRemote());
        assertThat(client)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_FULL, "http://localhost:8081/client/pong/one")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);

        SpanData clientServer = spans.ofKind(SERVER).childrenOf(client).single();
        verifyResource(clientServer);
        assertThat(clientServer).hasName("GET /client/pong/{message}");
        assertTrue(clientServer.hasEnded());
        assertTrue(clientServer.getParentSpanContext().isValid());
        assertTrue(clientServer.getParentSpanContext().isRemote());
        assertThat(clientServer)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/client/pong/one");
        assertThat(server)
                .hasAttribute(SERVER_ADDRESS, pathParamUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) pathParamUrl.getPort());
        assertThat(clientServer)
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_ROUTE, "/client/pong/{message}")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(clientServer.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(clientServer.getAttributes().get(USER_AGENT_ORIGINAL));
        assertThat(clientServer).hasParent(client);
    }

    @Test
    void testAsyncClientTracing() {
        given()
                .when().get("/client/async-ping/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData server = spans.ofKind(SERVER).roots().single();
        verifyResource(server);
        assertThat(server).hasName("GET /client/async-ping/{message}");
        assertTrue(server.hasEnded());
        assertEquals(SpanId.getInvalid(), server.getParentSpanContext().getSpanId());
        assertThat(server).hasNoParent();
        assertFalse(server.getParentSpanContext().isRemote());
        assertThat(server)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/client/async-ping/one")
                .hasAttribute(SERVER_ADDRESS, pathParamUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) pathParamUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_ROUTE, "/client/async-ping/{message}")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(server.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(server.getAttributes().get(USER_AGENT_ORIGINAL));

        SpanData client = spans.ofKind(CLIENT).childrenOf(server).single();
        assertThat(client).hasName("GET /client/pong/{message}");
        assertTrue(client.hasEnded());
        assertTrue(client.getParentSpanContext().isValid());
        assertFalse(client.getParentSpanContext().isRemote());
        assertThat(client)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_FULL, "http://localhost:8081/client/pong/one")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);

        SpanData clientServer = spans.ofKind(SERVER).childrenOf(client).single();
        verifyResource(clientServer);
        assertThat(clientServer).hasName("GET /client/pong/{message}");
        assertTrue(clientServer.hasEnded());
        assertTrue(clientServer.getParentSpanContext().isValid());
        assertTrue(clientServer.getParentSpanContext().isRemote());
        assertThat(clientServer)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/client/pong/one");
        assertThat(server)
                .hasAttribute(SERVER_ADDRESS, pathParamUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) pathParamUrl.getPort());
        assertThat(clientServer)
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_ROUTE, "/client/pong/{message}")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(clientServer.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(clientServer.getAttributes().get(USER_AGENT_ORIGINAL));
    }

    @Test
    void testClientTracingWithInterceptor() {
        given()
                .when().get("/client/pong-intercept/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        SpanDataList spans = spanExporter().awaitCount(4, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData server = spans.ofKind(SERVER).roots().single();
        verifyResource(server);
        assertThat(server).hasName("GET /client/pong-intercept/{message}");
        assertTrue(server.hasEnded());
        assertEquals(SpanId.getInvalid(), server.getParentSpanContext().getSpanId());
        assertThat(server).hasNoParent();
        assertFalse(server.getParentSpanContext().isRemote());
        assertThat(server)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/client/pong-intercept/one")
                .hasAttribute(SERVER_ADDRESS, pathParamUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) pathParamUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_ROUTE, "/client/pong-intercept/{message}")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(server.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(server.getAttributes().get(USER_AGENT_ORIGINAL));

        SpanData fromInterceptor = spans.ofKind(INTERNAL).childrenOf(server).single();
        assertThat(fromInterceptor).hasName("PingPongRestClient.pingpongIntercept");
        assertTrue(fromInterceptor.hasEnded());
        assertTrue(fromInterceptor.getParentSpanContext().isValid());
        assertFalse(fromInterceptor.getParentSpanContext().isRemote());
        assertThat(fromInterceptor.getAttributes())
                .doesNotContainKey(HTTP_REQUEST_METHOD.getKey())
                .doesNotContainKey(HTTP_RESPONSE_STATUS_CODE.getKey());
        assertThat(fromInterceptor).hasAttribute(stringKey("message"), "one");
        assertThat(fromInterceptor).hasAttribute(CODE_FUNCTION_NAME,
                "io.quarkus.it.opentelemetry.PingPongResource$PingPongRestClient.pingpongIntercept");

        SpanData client = spans.ofKind(CLIENT).childrenOf(fromInterceptor).single();
        assertThat(client).hasName("GET /client/pong/{message}");
        assertTrue(client.hasEnded());
        assertTrue(client.getParentSpanContext().isValid());
        assertFalse(client.getParentSpanContext().isRemote());
        assertThat(client)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_FULL, "http://localhost:8081/client/pong/one")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);

        SpanData clientServer = spans.ofKind(SERVER).childrenOf(client).single();
        verifyResource(clientServer);
        assertThat(clientServer).hasName("GET /client/pong/{message}");
        assertTrue(clientServer.hasEnded());
        assertTrue(clientServer.getParentSpanContext().isValid());
        assertTrue(clientServer.getParentSpanContext().isRemote());
        assertThat(clientServer)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/client/pong/one");
        assertThat(server)
                .hasAttribute(SERVER_ADDRESS, pathParamUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) pathParamUrl.getPort());
        assertThat(clientServer)
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_ROUTE, "/client/pong/{message}")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(clientServer.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(clientServer.getAttributes().get(USER_AGENT_ORIGINAL));
        assertThat(clientServer).hasParent(client);
    }

    @Test
    void testClientTracingWithInheritedInterceptedMethod() {
        given()
                .when().get("/client/pong-intercept-inherited/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        SpanDataList spans = spanExporter().awaitCount(4, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData server = spans.ofKind(SERVER).roots().single();
        assertThat(server).hasName("GET /client/pong-intercept-inherited/{message}");

        // the @WithSpan method is declared by the super interface of the REST client, which is what the span name
        // and the code.function.name attribute must report, as they do for any other CDI bean
        SpanData fromInterceptor = spans.ofKind(INTERNAL).childrenOf(server).single();
        assertThat(fromInterceptor).hasName("PingPongBaseRestClient.pingpongInherited");
        assertTrue(fromInterceptor.hasEnded());
        assertTrue(fromInterceptor.getParentSpanContext().isValid());
        assertFalse(fromInterceptor.getParentSpanContext().isRemote());
        assertThat(fromInterceptor).hasAttribute(stringKey("message"), "one");
        assertThat(fromInterceptor).hasAttribute(CODE_FUNCTION_NAME,
                "io.quarkus.it.opentelemetry.PingPongResource$PingPongBaseRestClient.pingpongInherited");

        SpanData client = spans.ofKind(CLIENT).childrenOf(fromInterceptor).single();
        assertThat(client)
                .hasName("GET /client/pong/{message}")
                .hasAttribute(URL_FULL, "http://localhost:8081/client/pong/one");

        SpanData clientServer = spans.ofKind(SERVER).childrenOf(client).single();
        assertThat(clientServer)
                .hasName("GET /client/pong/{message}")
                .hasParent(client);
    }

    @Test
    void testTemplatedPathOnClass() {
        given()
                .contentType("application/json")
                .when().get("/template/path/something")
                .then()
                .statusCode(200)
                .body(containsString("Received: something"));

        SpanData spanData = spanExporter().awaitCount(1, Duration.ofSeconds(10)).single();
        assertNotNull(spanData.getSpanId());

        verifyResource(spanData);

        assertThat(spanData)
                .hasName("GET /template/path/{value}")
                .hasKind(SERVER);
        assertTrue(spanData.hasEnded());

        assertEquals(SpanId.getInvalid(), spanData.getParentSpanContext().getSpanId());
        assertThat(spanData).hasNoParent();
        assertFalse(spanData.getParentSpanContext().isRemote());

        assertThat(spanData)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/template/path/something")
                .hasAttribute(SERVER_ADDRESS, deepPathUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) deepPathUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertNotNull(spanData.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(spanData.getAttributes().get(USER_AGENT_ORIGINAL));
    }

    @Test
    void testCustomSpanNames() {
        given()
                .contentType("application/json")
                .when().get("/client/async-ping-named/one")
                .then()
                .statusCode(200)
                .body(containsString("one"));

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofSeconds(10));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData server = spans.ofKind(SERVER).roots().single();
        SpanData client = spans.ofKind(CLIENT).childrenOf(server).single();

        verifyResource(client);
        assertThat(client).hasName("Async Ping");
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
        spanExporter().awaitCount(1, Duration.ofSeconds(10));
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
        SpanDataList spans = spanExporter().awaitUntil("more than 1 span", Duration.ofSeconds(5), s -> s.size() > 1);
        assertThat(spans).allSatisfy(span -> assertThat(span.getAttributes())
                .doesNotContainKey(ENDUSER_ID.getKey())
                .doesNotContainKey(USER_ROLES.getKey()));
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
            await().atMost(5, SECONDS).until(() -> !spanExporter().fetch().isEmpty());
        });
    }

    private void verifyResource(SpanData spanData) {
        // the configured resource attribute wins over the application name
        assertEquals("opentelemetry-integration-service", spanData.getResource().getAttribute(SERVICE_NAME));
        assertEquals("42", spanData.getResource().getAttribute(SERVICE_INSTANCE_ID));
        assertEquals("999-SNAPSHOT", spanData.getResource().getAttribute(SERVICE_VERSION));
        assertEquals("java", spanData.getResource().getAttribute(TELEMETRY_SDK_LANGUAGE));
        assertEquals("opentelemetry", spanData.getResource().getAttribute(TELEMETRY_SDK_NAME));
        assertNotNull(spanData.getResource().getAttribute(TELEMETRY_SDK_VERSION));
    }

    /**
     * In JVM mode the test runs in the same JVM as the application, so the OpenTelemetry instance
     * built by Quarkus is the one that must be used. It is looked up programmatically because
     * {@code @Inject} is rejected on any test whose class hierarchy is shared with a
     * {@code @QuarkusIntegrationTest}.
     * <p>
     * Private on purpose: an integration test runs in a separate JVM from the application and has no
     * CDI container, so subclasses must override {@link #createParentSpan()} and
     * {@link #parentSpanPropagator()} rather than reach for a bean here.
     */
    private OpenTelemetry openTelemetry() {
        return Arc.container().instance(OpenTelemetry.class).get();
    }

    protected Span createParentSpan() {
        return openTelemetry().getTracer("io.quarkus.opentelemetry")
                .spanBuilder("testTracingWithParentHeaders")
                .setNoParent()
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();
    }

    protected TextMapPropagator parentSpanPropagator() {
        return openTelemetry().getPropagators().getTextMapPropagator();
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
