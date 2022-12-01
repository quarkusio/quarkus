package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class OpenTelemetryReactiveTest {
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 0);
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

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 2);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(2, spans.size());
        assertEquals(spans.get(0).get("traceId"), spans.get(1).get("traceId"));
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

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 2);
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

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 7);

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
                .filter(map -> ((String) ((Map<?, ?>) map.get("attributes")).get(HTTP_URL.getKey())).contains("Naruto"))
                .findFirst();
        assertTrue(narutoSpan.isPresent());
        Map<String, Object> naruto = narutoSpan.get();

        Map<String, Object> narutoServer = getSpanByKindAndParentId(spans, SERVER, naruto.get("spanId"));
        assertEquals("/reactive?name=Naruto", ((Map<?, ?>) narutoServer.get("attributes")).get(HTTP_TARGET.getKey()));
        Map<String, Object> narutoInternal = getSpanByKindAndParentId(spans, INTERNAL, narutoServer.get("spanId"));
        assertEquals("helloGet", narutoInternal.get("name"));

        // Goku Span
        Optional<Map<String, Object>> gokuSpan = clientSpans.stream()
                .filter(map -> ((String) ((Map<?, ?>) map.get("attributes")).get(HTTP_URL.getKey())).contains("Goku"))
                .findFirst();
        assertTrue(gokuSpan.isPresent());
        Map<String, Object> goku = gokuSpan.get();

        Map<String, Object> gokuServer = getSpanByKindAndParentId(spans, SERVER, goku.get("spanId"));
        assertEquals("/reactive?name=Goku", ((Map<?, ?>) gokuServer.get("attributes")).get(HTTP_TARGET.getKey()));
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

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 7);

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
                .filter(map -> ((String) ((Map<?, ?>) map.get("attributes")).get(HTTP_URL.getKey())).contains("Naruto"))
                .findFirst();
        assertTrue(narutoSpan.isPresent());
        Map<String, Object> naruto = narutoSpan.get();

        Map<String, Object> narutoServer = getSpanByKindAndParentId(spans, SERVER, naruto.get("spanId"));
        assertEquals("/reactive?name=Naruto", ((Map<?, ?>) narutoServer.get("attributes")).get(HTTP_TARGET.getKey()));
        Map<String, Object> narutoInternal = getSpanByKindAndParentId(spans, INTERNAL, narutoServer.get("spanId"));
        assertEquals("helloGet", narutoInternal.get("name"));

        // Goku Span
        Optional<Map<String, Object>> gokuSpan = clientSpans.stream()
                .filter(map -> ((String) ((Map<?, ?>) map.get("attributes")).get(HTTP_URL.getKey())).contains("Goku"))
                .findFirst();
        assertTrue(gokuSpan.isPresent());
        Map<String, Object> goku = gokuSpan.get();

        Map<String, Object> gokuServer = getSpanByKindAndParentId(spans, SERVER, goku.get("spanId"));
        assertEquals("/reactive?name=Goku", ((Map<?, ?>) gokuServer.get("attributes")).get(HTTP_TARGET.getKey()));
        Map<String, Object> gokuInternal = getSpanByKindAndParentId(spans, INTERNAL, gokuServer.get("spanId"));
        assertEquals("helloGet", gokuInternal.get("name"));
    }

    private static List<Map<String, Object>> getSpans() {
        return when().get("/export").body().as(new TypeRef<>() {
        });
    }

    private static Map<String, Object> getSpanByKindAndParentId(List<Map<String, Object>> spans, SpanKind kind,
            Object parentSpanId) {
        List<Map<String, Object>> span = getSpansByKindAndParentId(spans, kind, parentSpanId);
        assertEquals(1, span.size());
        return span.get(0);
    }

    private static List<Map<String, Object>> getSpansByKindAndParentId(List<Map<String, Object>> spans, SpanKind kind,
            Object parentSpanId) {
        return spans.stream()
                .filter(map -> map.get("kind").equals(kind.toString()))
                .filter(map -> map.get("parentSpanId").equals(parentSpanId)).collect(toList());
    }
}
