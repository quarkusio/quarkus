package io.quarkus.it.rest.client;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.with;
import static java.util.stream.Collectors.counting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceId;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

@QuarkusTest
@TestProfile(BasicTest.TestProfile.class)
public class BasicTest {

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
        RestAssured.given().when().get("/export-clear").then().statusCode(200);
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

        String serverSpanId = null;
        String serverTraceId = null;
        String clientSpanId = null;

        Awaitility.await().atMost(Duration.ofSeconds(30))
                .until(() -> getServerSpansFromPath("POST /call-hello-client-trace", "/call-hello-client-trace").size() > 0);

        List<Map<String, Object>> spans = getServerSpansFromPath("POST /call-hello-client-trace", "/call-hello-client-trace");
        Assertions.assertEquals(1, spans.size());

        final Map<String, Object> initialServerSpan = spans.get(0);
        Assertions.assertNotNull(initialServerSpan);
        Assertions.assertNotNull(initialServerSpan.get("spanId"));

        // *** Server Span ***
        serverSpanId = (String) initialServerSpan.get("spanId");
        serverTraceId = (String) initialServerSpan.get("traceId");

        Assertions.assertEquals("POST /call-hello-client-trace", initialServerSpan.get("name"));
        Assertions.assertEquals(SpanKind.SERVER.toString(), initialServerSpan.get("kind"));
        Assertions.assertTrue((Boolean) initialServerSpan.get("ended"));

        Assertions.assertEquals(SpanId.getInvalid(), initialServerSpan.get("parent_spanId"));
        Assertions.assertEquals(TraceId.getInvalid(), initialServerSpan.get("parent_traceId"));
        Assertions.assertFalse((Boolean) initialServerSpan.get("parent_valid"));
        Assertions.assertFalse((Boolean) initialServerSpan.get("parent_remote"));

        Assertions.assertEquals("POST", initialServerSpan.get("attr_http.request.method"));
        Assertions.assertEquals("/call-hello-client-trace", initialServerSpan.get("attr_url.path"));
        Assertions.assertEquals("http", initialServerSpan.get("attr_url.scheme"));
        Assertions.assertEquals("200", initialServerSpan.get("attr_http.response.status_code"));
        Assertions.assertNotNull(initialServerSpan.get("attr_client.address"));
        Assertions.assertNotNull(initialServerSpan.get("attr_user_agent.original"));

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .until(() -> getClientSpansFromFullUrl("POST", "http://localhost:8081/hello?count=3").size() > 0);

        spans = getClientSpansFromFullUrl("POST", "http://localhost:8081/hello?count=3");
        Assertions.assertEquals(1, spans.size());

        final Map<String, Object> clientSpan = spans.get(0);
        Assertions.assertNotNull(clientSpan);
        Assertions.assertNotNull(clientSpan.get("spanId"));

        // *** Client span ***
        Assertions.assertEquals("POST", clientSpan.get("name"));

        Assertions.assertEquals(SpanKind.CLIENT.toString(), clientSpan.get("kind"));
        Assertions.assertTrue((Boolean) clientSpan.get("ended"));

        if (serverSpanId != null) {
            Assertions.assertEquals(serverSpanId, clientSpan.get("parent_spanId"));
        }
        if (serverTraceId != null) {
            Assertions.assertEquals(serverTraceId, clientSpan.get("parent_traceId"));
        }
        Assertions.assertTrue((Boolean) clientSpan.get("parent_valid"));
        Assertions.assertFalse((Boolean) clientSpan.get("parent_remote"));

        Assertions.assertEquals("POST", clientSpan.get("attr_http.request.method"));
        Assertions.assertEquals("http://localhost:8081/hello?count=3", clientSpan.get("attr_url.full"));
        Assertions.assertEquals("200", clientSpan.get("attr_http.response.status_code"));

        clientSpanId = (String) clientSpan.get("spanId");

        Awaitility.await().atMost(Duration.ofSeconds(5))
                .until(() -> getServerSpansFromPath("POST /hello", "/hello").size() > 0);
        spans = getServerSpansFromPath("POST /hello", "/hello");
        Assertions.assertEquals(1, spans.size(), "found: " + spans);

        final Map<String, Object> serverSpanClientSide = spans.get(0);
        Assertions.assertNotNull(serverSpanClientSide);
        Assertions.assertNotNull(serverSpanClientSide.get("spanId"));

        // *** Server span of client ***
        Assertions.assertEquals("POST /hello", serverSpanClientSide.get("name"));
        Assertions.assertEquals(SpanKind.SERVER.toString(), serverSpanClientSide.get("kind"));
        Assertions.assertTrue((Boolean) serverSpanClientSide.get("ended"));

        if (clientSpanId != null) {
            Assertions.assertEquals(clientSpanId, serverSpanClientSide.get("parent_spanId"));
        }
        if (serverTraceId != null) {
            Assertions.assertEquals(serverTraceId, serverSpanClientSide.get("parent_traceId"));
        }
        Assertions.assertTrue((Boolean) serverSpanClientSide.get("parent_valid"));
        Assertions.assertTrue((Boolean) serverSpanClientSide.get("parent_remote"));

        Assertions.assertEquals("POST", serverSpanClientSide.get("attr_http.request.method"));
        Assertions.assertEquals("/hello", serverSpanClientSide.get("attr_url.path"));
        Assertions.assertEquals("count=3", serverSpanClientSide.get("attr_url.query"));
        Assertions.assertEquals("http", serverSpanClientSide.get("attr_url.scheme"));
        Assertions.assertEquals("200", serverSpanClientSide.get("attr_http.response.status_code"));
        Assertions.assertNotNull(serverSpanClientSide.get("attr_client.address"));
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

    private List<Map<String, Object>> getServerSpansFromPath(final String spanName, final String urlPath) {
        return get("/export").body().as(new TypeRef<List<Map<String, Object>>>() {
        }).stream()
                .filter(stringObjectMap -> spanName.equals(stringObjectMap.get("name")) &&
                        "SERVER".equals(stringObjectMap.get("kind")) &&
                        ((String) stringObjectMap.get("attr_url.path")).startsWith(urlPath))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getClientSpansFromFullUrl(final String spanName, final String httpUrl) {
        return get("/export").body().as(new TypeRef<List<Map<String, Object>>>() {
        }).stream()
                .filter(stringObjectMap -> spanName.equals(stringObjectMap.get("name")) &&
                        "CLIENT".equals(stringObjectMap.get("kind")) &&
                        ((String) stringObjectMap.get("attr_url.full")).startsWith(httpUrl))
                .collect(Collectors.toList());
    }

    public static class TestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("w-fault-tolerance-int/mp-rest/url", "${test.url}");
        }
    }
}
