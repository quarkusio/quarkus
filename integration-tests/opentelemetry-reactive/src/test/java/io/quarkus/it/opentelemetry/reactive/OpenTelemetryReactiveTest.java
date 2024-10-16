package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.UrlAttributes.URL_FULL;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static io.quarkus.it.opentelemetry.reactive.Utils.getExceptionEventData;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpanByKindAndParentId;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpanEventAttrs;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpans;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpansByKindAndParentId;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.opentelemetry.runtime.tracing.security.SecurityEventUtil;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OpenTelemetryReactiveTest {

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

    @Test
    void get() {
        given()
                .when()
                .queryParam("name", "Naruto")
                .get("/reactive")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Naruto"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 2);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(2, spans.size());
        assertEquals(spans.get(0).get("traceId"), spans.get(1).get("traceId"));
    }

    @Test
    void helloGetUniDelayTest() {
        given()
                .when()
                .get("/reactive/hello-get-uni-delay")
                .then()
                .statusCode(200)
                .body(equalTo("helloGetUniDelay"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 2);
        Map<String, Object> parent = getSpanByKindAndParentId(getSpans(), SERVER, "0000000000000000");
        assertEquals("GET /reactive/hello-get-uni-delay", parent.get("name"));

        Map<String, Object> child = getSpanByKindAndParentId(getSpans(), INTERNAL, parent.get("spanId"));
        assertEquals("helloGetUniDelay", child.get("name"));

        assertEquals(child.get("traceId"), parent.get("traceId"));
    }

    @Test
    void helloGetUniExecutorTest() {
        given()
                .when()
                .get("/reactive/hello-get-uni-executor")
                .then()
                .statusCode(200)
                .body(equalTo("helloGetUniExecutor"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 2);
        Map<String, Object> parent = getSpanByKindAndParentId(getSpans(), SERVER, "0000000000000000");
        assertEquals("GET /reactive/hello-get-uni-executor", parent.get("name"));

        Map<String, Object> child = getSpanByKindAndParentId(getSpans(), INTERNAL, parent.get("spanId"));
        assertEquals("helloGetUniExecutor", child.get("name"));

        assertEquals(child.get("traceId"), parent.get("traceId"));
    }

    @Test
    void blockingException() {
        given()
                .when()
                .get("/reactive/blockingException")
                .then()
                .statusCode(500);

        assertExceptionRecorded();
    }

    @Test
    void reactiveException() {
        given()
                .when()
                .get("/reactive/reactiveException")
                .then()
                .statusCode(500);

        assertExceptionRecorded();
    }

    private static void assertExceptionRecorded() {
        await().atMost(5, SECONDS).until(() -> getExceptionEventData().size() == 1);
        assertThat(getExceptionEventData()).singleElement().satisfies(s -> {
            assertThat(s).contains("dummy");
        });
    }

    @Test
    void post() {
        given()
                .when()
                .body("Naruto")
                .post("/reactive")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Naruto"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 2);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(2, spans.size());
        assertEquals(spans.get(0).get("traceId"), spans.get(1).get("traceId"));
    }

    @Test
    void multipleUsingChain() {
        given()
                .when()
                .get("/reactive/multiple-chain")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Naruto and Hello Goku"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 7);

        List<Map<String, Object>> spans = getSpans();
        assertEquals(7, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        // First span is the call getting into the server. It does not have a parent span.
        Map<String, Object> parent = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");

        // We should get 2 client spans originated by the server
        List<Map<String, Object>> clientSpans = getSpansByKindAndParentId(spans, CLIENT, parent.get("spanId"));
        assertEquals(2, clientSpans.size());

        // Each client calls the server and programmatically create a span, so each have a server and an internal span

        // Naruto Span
        Optional<Map<String, Object>> narutoSpan = clientSpans.stream()
                .filter(map -> ((String) ((Map<?, ?>) map.get("attributes")).get(URL_FULL.getKey())).contains("Naruto"))
                .findFirst();
        assertTrue(narutoSpan.isPresent());
        Map<String, Object> naruto = narutoSpan.get();

        Map<String, Object> narutoServer = getSpanByKindAndParentId(spans, SERVER, naruto.get("spanId"));
        assertEquals("/reactive", ((Map<?, ?>) narutoServer.get("attributes")).get(URL_PATH.getKey()));
        assertEquals("name=Naruto", ((Map<?, ?>) narutoServer.get("attributes")).get(URL_QUERY.getKey()));
        Map<String, Object> narutoInternal = getSpanByKindAndParentId(spans, INTERNAL, narutoServer.get("spanId"));
        assertEquals("helloGet", narutoInternal.get("name"));

        // Goku Span
        Optional<Map<String, Object>> gokuSpan = clientSpans.stream()
                .filter(map -> ((String) ((Map<?, ?>) map.get("attributes")).get(URL_FULL.getKey())).contains("Goku"))
                .findFirst();
        assertTrue(gokuSpan.isPresent());
        Map<String, Object> goku = gokuSpan.get();

        Map<String, Object> gokuServer = getSpanByKindAndParentId(spans, SERVER, goku.get("spanId"));
        assertEquals("/reactive", ((Map<?, ?>) gokuServer.get("attributes")).get(URL_PATH.getKey()));
        assertEquals("name=Goku", ((Map<?, ?>) gokuServer.get("attributes")).get(URL_QUERY.getKey()));
        Map<String, Object> gokuInternal = getSpanByKindAndParentId(spans, INTERNAL, gokuServer.get("spanId"));
        assertEquals("helloGet", gokuInternal.get("name"));
    }

    @Test
    void multipleUsingCombine() {
        given()
                .when()
                .get("/reactive/multiple-combine")
                .then()
                .statusCode(200)
                .body(equalTo("Hello Naruto and Hello Goku"));

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 7);

        List<Map<String, Object>> spans = getSpans();
        assertEquals(7, spans.size());
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        // First span is the call getting into the server. It does not have a parent span.
        Map<String, Object> parent = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");

        // We should get 2 client spans originated by the server
        List<Map<String, Object>> clientSpans = getSpansByKindAndParentId(spans, CLIENT, parent.get("spanId"));
        assertEquals(2, clientSpans.size());

        // Each client calls the server and programmatically create a span, so each have a server and an internal span

        // Naruto Span
        Optional<Map<String, Object>> narutoSpan = clientSpans.stream()
                .filter(map -> ((String) ((Map<?, ?>) map.get("attributes")).get(URL_FULL.getKey())).contains("Naruto"))
                .findFirst();
        assertTrue(narutoSpan.isPresent());
        Map<String, Object> naruto = narutoSpan.get();

        Map<String, Object> narutoServer = getSpanByKindAndParentId(spans, SERVER, naruto.get("spanId"));
        assertEquals("/reactive", ((Map<?, ?>) narutoServer.get("attributes")).get(URL_PATH.getKey()));
        assertEquals("name=Naruto", ((Map<?, ?>) narutoServer.get("attributes")).get(URL_QUERY.getKey()));
        Map<String, Object> narutoInternal = getSpanByKindAndParentId(spans, INTERNAL, narutoServer.get("spanId"));
        assertEquals("helloGet", narutoInternal.get("name"));

        // Goku Span
        Optional<Map<String, Object>> gokuSpan = clientSpans.stream()
                .filter(map -> ((String) ((Map<?, ?>) map.get("attributes")).get(URL_FULL.getKey())).contains("Goku"))
                .findFirst();
        assertTrue(gokuSpan.isPresent());
        Map<String, Object> goku = gokuSpan.get();

        Map<String, Object> gokuServer = getSpanByKindAndParentId(spans, SERVER, goku.get("spanId"));
        assertEquals("/reactive", ((Map<?, ?>) gokuServer.get("attributes")).get(URL_PATH.getKey()));
        assertEquals("name=Goku", ((Map<?, ?>) gokuServer.get("attributes")).get(URL_QUERY.getKey()));
        Map<String, Object> gokuInternal = getSpanByKindAndParentId(spans, INTERNAL, gokuServer.get("spanId"));
        assertEquals("helloGet", gokuInternal.get("name"));
    }

    @Test
    public void securedInvalidCredential() {
        given().auth().preemptive().basic("scott", "reader2").when().get("/foo/secured/item/something")
                .then()
                .statusCode(401);

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
        assertThat(getSpans()).singleElement().satisfies(m -> {
            assertThat(m).extractingByKey("name").isEqualTo("GET /{dummy}/secured/item/{value}");
            assertEvent(m, SecurityEventUtil.AUTHN_FAILURE_EVENT_NAME);
        });
    }

    @Test
    public void securedProperCredentials() {
        given().auth().preemptive().basic("scott", "reader").when().get("/foo/secured/item/something")
                .then()
                .statusCode(200);

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
        assertThat(getSpans()).singleElement().satisfies(m -> {
            assertThat(m).extractingByKey("name").isEqualTo("GET /{dummy}/secured/item/{value}");
            assertEvent(m, SecurityEventUtil.AUTHN_SUCCESS_EVENT_NAME, SecurityEventUtil.AUTHZ_SUCCESS_EVENT_NAME);
        });
    }

    private static void assertEvent(Map<String, Object> spanData, String... expectedEventNames) {
        String spanName = (String) spanData.get("name");
        var events = (List) spanData.get("events");
        Assertions.assertEquals(expectedEventNames.length, events.size());
        for (String expectedEventName : expectedEventNames) {
            boolean foundEvent = events.stream().anyMatch(m -> expectedEventName.equals(((Map) m).get("name")));
            assertTrue(foundEvent, "Span '%s' did not contain event '%s'".formatted(spanName, expectedEventName));
            assertEventAttributes(spanName, expectedEventName);
        }
    }

    private static void assertEventAttributes(String spanName, String eventName) {
        var attrs = getSpanEventAttrs(spanName, eventName);
        switch (eventName) {
            case SecurityEventUtil.AUTHN_FAILURE_EVENT_NAME:
                assertEquals(AuthenticationFailedException.class.getName(), attrs.get(SecurityEventUtil.FAILURE_NAME));
                break;
            case SecurityEventUtil.AUTHN_SUCCESS_EVENT_NAME:
            case SecurityEventUtil.AUTHZ_SUCCESS_EVENT_NAME:
                assertEquals("scott", attrs.get(SecurityEventUtil.SECURITY_IDENTITY_PRINCIPAL));
                assertEquals(Boolean.FALSE, attrs.get(SecurityEventUtil.SECURITY_IDENTITY_IS_ANONYMOUS));
                break;
            default:
                Assertions.fail("Unknown event name " + eventName);
        }
    }
}
