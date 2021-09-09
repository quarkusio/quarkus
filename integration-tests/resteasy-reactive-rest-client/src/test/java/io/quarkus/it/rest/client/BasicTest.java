package io.quarkus.it.rest.client;

import static io.restassured.RestAssured.get;
import static java.util.stream.Collectors.counting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceId;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

@QuarkusTest
public class BasicTest {

    @TestHTTPResource("/apples")
    String appleUrl;
    @TestHTTPResource()
    String baseUrl;

    @TestHTTPResource("/hello")
    String helloUrl;

    @Test
    public void shouldMakeTextRequest() {
        Response response = RestAssured.with().body(helloUrl).post("/call-hello-client");
        assertThat(response.asString()).isEqualTo("Hello, JohnJohn");
    }

    @Test
    public void restResponseShouldWorkWithNonSuccessfulResponse() {
        Response response = RestAssured.with().body(helloUrl).post("/rest-response");
        assertThat(response.asString()).isEqualTo("405");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void shouldMakeJsonRequest() {
        List<Map> results = RestAssured.with().body(appleUrl).post("/call-client")
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
        RestAssured.with().body(appleUrl).post("/call-client-retry")
                .then()
                .statusCode(200)
                .body(equalTo("4"));
    }

    @Test
    void shouldMapException() {
        RestAssured.with().body(baseUrl).post("/call-client-with-exception-mapper")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldMapExceptionCdi() {
        RestAssured.with().body(baseUrl).post("/call-cdi-client-with-exception-mapper")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldCreateClientSpans() {
        // Reset captured traces
        RestAssured.given().when().get("/export-clear").then().statusCode(200);

        Response response = RestAssured.with().body(helloUrl).post("/call-hello-client");
        assertThat(response.asString()).isEqualTo("Hello, JohnJohn");

        Awaitility.await().atMost(Duration.ofMinutes(2)).until(() -> getSpans().size() == 3);

        boolean outsideServerFound = false;
        boolean clientFound = false;
        boolean clientServerFound = false;

        String serverSpanId = null;
        String serverTraceId = null;
        String clientSpanId = null;

        for (Map<String, Object> spanData : getSpans()) {
            Assertions.assertNotNull(spanData);
            Assertions.assertNotNull(spanData.get("spanId"));

            if (spanData.get("kind").equals(SpanKind.SERVER.toString())
                    && spanData.get("name").equals("call-hello-client")) {
                outsideServerFound = true;
                // Server Span
                serverSpanId = (String) spanData.get("spanId");
                serverTraceId = (String) spanData.get("traceId");

                Assertions.assertEquals("call-hello-client", spanData.get("name"));
                Assertions.assertEquals(SpanKind.SERVER.toString(), spanData.get("kind"));
                Assertions.assertTrue((Boolean) spanData.get("ended"));

                Assertions.assertEquals(SpanId.getInvalid(), spanData.get("parent_spanId"));
                Assertions.assertEquals(TraceId.getInvalid(), spanData.get("parent_traceId"));
                Assertions.assertFalse((Boolean) spanData.get("parent_valid"));
                Assertions.assertFalse((Boolean) spanData.get("parent_remote"));

                Assertions.assertEquals("POST", spanData.get("attr_http.method"));
                Assertions.assertEquals("1.1", spanData.get("attr_http.flavor"));
                Assertions.assertEquals("/call-hello-client", spanData.get("attr_http.target"));
                Assertions.assertEquals("http", spanData.get("attr_http.scheme"));
                Assertions.assertEquals("200", spanData.get("attr_http.status_code"));
                Assertions.assertNotNull(spanData.get("attr_http.client_ip"));
                Assertions.assertNotNull(spanData.get("attr_http.user_agent"));
            } else if (spanData.get("kind").equals(SpanKind.CLIENT.toString())
                    && spanData.get("name").equals("hello")) {
                clientFound = true;
                // Client span

                Assertions.assertEquals("hello", spanData.get("name"));
                Assertions.assertEquals(SpanKind.CLIENT.toString(), spanData.get("kind"));
                Assertions.assertTrue((Boolean) spanData.get("ended"));

                if (serverSpanId != null) {
                    Assertions.assertEquals(serverSpanId, spanData.get("parent_spanId"));
                }
                if (serverTraceId != null) {
                    Assertions.assertEquals(serverTraceId, spanData.get("parent_traceId"));
                }
                Assertions.assertTrue((Boolean) spanData.get("parent_valid"));
                Assertions.assertFalse((Boolean) spanData.get("parent_remote"));

                Assertions.assertEquals("POST", spanData.get("attr_http.method"));
                Assertions.assertEquals("http://localhost:8081/hello?count=2", spanData.get("attr_http.url"));
                Assertions.assertEquals("200", spanData.get("attr_http.status_code"));

                clientSpanId = (String) spanData.get("spanId");
            } else if (spanData.get("kind").equals(SpanKind.SERVER.toString())
                    && spanData.get("name").equals("hello?count=2")) {
                clientServerFound = true;
                // Server span of client

                Assertions.assertEquals("hello?count=2", spanData.get("name"));
                Assertions.assertEquals(SpanKind.SERVER.toString(), spanData.get("kind"));
                Assertions.assertTrue((Boolean) spanData.get("ended"));

                if (clientSpanId != null) {
                    Assertions.assertEquals(clientSpanId, spanData.get("parent_spanId"));
                }
                if (serverTraceId != null) {
                    Assertions.assertEquals(serverTraceId, spanData.get("parent_traceId"));
                }
                Assertions.assertTrue((Boolean) spanData.get("parent_valid"));
                Assertions.assertTrue((Boolean) spanData.get("parent_remote"));

                Assertions.assertEquals("POST", spanData.get("attr_http.method"));
                Assertions.assertEquals("1.1", spanData.get("attr_http.flavor"));
                Assertions.assertEquals("/hello", spanData.get("attr_http.target"));
                Assertions.assertEquals("http", spanData.get("attr_http.scheme"));
                Assertions.assertEquals("200", spanData.get("attr_http.status_code"));
                Assertions.assertNotNull(spanData.get("attr_http.client_ip"));
            } else {
                Assertions.fail("Received an unknown Span - " + spanData.get("name"));
            }
        }

        Assertions.assertTrue(outsideServerFound);
        Assertions.assertTrue(clientFound);
        Assertions.assertTrue(clientServerFound);
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<List<Map<String, Object>>>() {
        });
    }
}
