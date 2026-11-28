package io.quarkus.it.rest.client;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.ClientAttributes.CLIENT_ADDRESS;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.UrlAttributes.URL_FULL;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static io.opentelemetry.semconv.UrlAttributes.URL_SCHEME;
import static io.opentelemetry.semconv.UserAgentAttributes.USER_AGENT_ORIGINAL;
import static io.restassured.RestAssured.with;
import static java.util.stream.Collectors.counting;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.response.Response;

@QuarkusTest
@TestProfile(BasicTest.TestProfile.class)
public class BasicTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @TestHTTPResource("/apples")
    String appleUrl;
    @TestHTTPResource()
    String baseUrl;
    @TestHTTPResource("/hello")
    String helloUrl;
    @TestHTTPResource("/params")
    String paramsUrl;

    @BeforeEach
    @AfterEach
    public void clear() {
        // Reset captured traces
        spanExporter().reset();
    }

    @Test
    public void shouldMakeTextRequest() {
        Response response = with().body(helloUrl).post("/call-hello-client");
        assertThat(response.asString()).isEqualTo("Hello, JohnJohn");
    }

    @Test
    public void shouldMakeJsonRequestAndGetTextResponse() {
        Response response = with().body(helloUrl).post("/call-helloFromMessage-client");
        assertThat(response.asString()).isEqualTo("Hello world");
    }

    @Test
    public void restResponseShouldWorkWithNonSuccessfulResponse() {
        Response response = with().body(helloUrl).post("/rest-response");
        assertThat(response.asString()).isEqualTo("405");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void shouldMakeJsonRequest() {
        List<Map> results = with().body(appleUrl).post("/call-client")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().jsonPath().getList(".", Map.class);
        assertThat(results).hasSize(11).allSatisfy(m -> {
            assertThat(m).containsOnlyKeys("cultivar");
        });
        Map<Object, Long> valueByCount = results.stream().collect(Collectors.groupingBy(m -> m.get("cultivar"), counting()));
        assertThat(valueByCount).containsOnly(entry("cortland", 4L), entry("lobo", 4L), entry("golden delicious", 3L));
    }

    @Test
    void shouldRetryOnFailure() {
        with().body(appleUrl).post("/call-client-retry")
                .then()
                .statusCode(200)
                .body(equalTo("4"));
    }

    @Test
    void shouldLogWithExplicitLogger() {
        with().body(baseUrl).post("/call-client-with-explicit-client-logger")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldLogWithGlobalLogger() {
        with().body(baseUrl).post("/call-client-with-global-client-logger")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldLogCdiWithGlobalLogger() {
        with().body(baseUrl).post("/call-cdi-client-with-global-client-logger")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldLogExactlyOnceWithMultipart() {
        with().body(baseUrl).post("/call-client-multipart")
                .then()
                .statusCode(200)
                // 1 the request + 1 the response
                .body(equalTo("logger was used 2 times"));
    }

    @Test
    void shouldLogExactlyOnceCdiWithMultipart() {
        with().body(baseUrl).post("/call-cdi-client-multipart")
                .then()
                .statusCode(200)
                // 1 the request + 1 the response
                .body(equalTo("logger was used 2 times"));
    }

    @Test
    void shouldMapException() {
        with().body(baseUrl).post("/call-client-with-exception-mapper")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldMapExceptionCdi() {
        with().body(baseUrl).post("/call-cdi-client-with-exception-mapper")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldInterceptDefaultMethod() {
        with().body(baseUrl).post("/call-with-fault-tolerance")
                .then()
                .statusCode(200)
                .body(equalTo("Hello fallback!"));
    }

    @Test
    void shouldApplyInterfaceLevelInterceptorBinding() {
        for (int i = 0; i < 2; i++) {
            with().body(baseUrl).post("/call-with-fault-tolerance-on-interface")
                    .then()
                    .statusCode(200)
                    .body(equalTo("ClientWebApplicationException"));
        }

        with().body(baseUrl).post("/call-with-fault-tolerance-on-interface")
                .then()
                .statusCode(200)
                .body(equalTo("CircuitBreakerOpenException"));
    }

    @Test
    void shouldCreateClientSpans() {
        Response response = with().body(helloUrl).post("/call-hello-client-trace");
        assertThat(response.asString()).isEqualTo("Hello, MaryMaryMary");

        SpanDataList spans = spanExporter().awaitUntil("a server span of POST /call-hello-client-trace",
                Duration.ofSeconds(30),
                s -> !getServerSpansFromPath(s, "POST /call-hello-client-trace", "/call-hello-client-trace").isEmpty());

        SpanData initialServerSpan = getServerSpansFromPath(spans, "POST /call-hello-client-trace",
                "/call-hello-client-trace").single();
        assertNotNull(initialServerSpan.getSpanId());

        // *** Server Span ***
        assertThat(initialServerSpan)
                .hasName("POST /call-hello-client-trace")
                .hasKind(SpanKind.SERVER)
                .hasNoParent()
                .hasAttribute(HTTP_REQUEST_METHOD, "POST")
                .hasAttribute(URL_PATH, "/call-hello-client-trace")
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertTrue(initialServerSpan.hasEnded());
        assertEquals(SpanId.getInvalid(), initialServerSpan.getParentSpanContext().getSpanId());
        assertFalse(initialServerSpan.getParentSpanContext().isRemote());
        assertNotNull(initialServerSpan.getAttributes().get(CLIENT_ADDRESS));
        assertNotNull(initialServerSpan.getAttributes().get(USER_AGENT_ORIGINAL));

        spans = spanExporter().awaitUntil("a client span of POST http://localhost:8081/hello?count=3", Duration.ofSeconds(5),
                s -> !getClientSpansFromFullUrl(s, "POST", "http://localhost:8081/hello?count=3").isEmpty());

        SpanData clientSpan = getClientSpansFromFullUrl(spans, "POST", "http://localhost:8081/hello?count=3")
                .single();
        assertNotNull(clientSpan.getSpanId());

        // *** Client span ***
        assertThat(clientSpan)
                .hasName("POST")
                .hasKind(SpanKind.CLIENT)
                .hasParent(initialServerSpan)
                .hasAttribute(HTTP_REQUEST_METHOD, "POST")
                .hasAttribute(URL_FULL, "http://localhost:8081/hello?count=3")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertTrue(clientSpan.hasEnded());
        assertEquals(initialServerSpan.getTraceId(), clientSpan.getParentSpanContext().getTraceId());
        assertTrue(clientSpan.getParentSpanContext().isValid());
        assertFalse(clientSpan.getParentSpanContext().isRemote());

        spans = spanExporter().awaitUntil("a server span of POST /hello", Duration.ofSeconds(5),
                s -> !getServerSpansFromPath(s, "POST /hello", "/hello").isEmpty());

        SpanData serverSpanClientSide = getServerSpansFromPath(spans, "POST /hello", "/hello").single();
        assertNotNull(serverSpanClientSide.getSpanId());

        // *** Server span of client ***
        assertThat(serverSpanClientSide)
                .hasName("POST /hello")
                .hasKind(SpanKind.SERVER)
                .hasParent(clientSpan)
                .hasTraceId(initialServerSpan.getTraceId())
                .hasAttribute(HTTP_REQUEST_METHOD, "POST")
                .hasAttribute(URL_PATH, "/hello")
                .hasAttribute(URL_QUERY, "count=3")
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        assertTrue(serverSpanClientSide.hasEnded());
        assertEquals(initialServerSpan.getTraceId(), serverSpanClientSide.getParentSpanContext().getTraceId());
        assertTrue(serverSpanClientSide.getParentSpanContext().isValid());
        assertTrue(serverSpanClientSide.getParentSpanContext().isRemote());
        assertNotNull(serverSpanClientSide.getAttributes().get(CLIENT_ADDRESS));
    }

    @Test
    public void shouldConvertParamFirstToOneUsingCustomConverter() {
        with().body(paramsUrl).post("/call-params-client-with-param-first")
                .then()
                .statusCode(200)
                .body(equalTo("1"));
    }

    @Test
    void shouldPreserveResponseEntity() {
        with().body(baseUrl).post("/preserve-response-entity")
                .then()
                .statusCode(200)
                .body(is(equalTo("true")));
    }

    @Test
    void shouldPreserveResponseEntityAsync() {
        with().body(baseUrl).post("/preserve-response-entity-async")
                .then()
                .log().all()
                .statusCode(200)
                .body(is(equalTo("true")));
    }

    private SpanDataList getServerSpansFromPath(SpanDataList spans, final String spanName, final String urlPath) {
        return spans.ofKind(SpanKind.SERVER).named(spanName)
                .filter(span -> span.getAttributes().get(URL_PATH).startsWith(urlPath));
    }

    private SpanDataList getClientSpansFromFullUrl(SpanDataList spans, final String spanName, final String httpUrl) {
        return spans.ofKind(SpanKind.CLIENT).named(spanName)
                .filter(span -> span.getAttributes().get(URL_FULL).startsWith(httpUrl));
    }

    public static class TestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("w-fault-tolerance-int/mp-rest/url", "${test.url}");
        }
    }
}
