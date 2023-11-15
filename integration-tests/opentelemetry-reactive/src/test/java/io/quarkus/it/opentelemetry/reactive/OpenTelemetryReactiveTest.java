package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_TARGET;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_URL;
import static io.quarkus.it.opentelemetry.reactive.Utils.getExceptionEventData;
import static io.quarkus.it.opentelemetry.reactive.Utils.getSpanByKindAndParentId;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OpenTelemetryReactiveTest {

    @BeforeEach
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, SECONDS).until(() -> getSpans().size() == 0);
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
    public void securedInvalidCredential() {
        given().auth().preemptive().basic("scott", "reader2").when().get("/secured/item/something")
                .then()
                .statusCode(401);

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
        assertThat(getSpans()).singleElement().satisfies(m -> {
            assertThat(m).extractingByKey("name").isEqualTo("GET /secured/item/{value}");
        });
    }

    @Test
    public void securedProperCredentials() {
        given().auth().preemptive().basic("scott", "reader").when().get("/secured/item/something")
                .then()
                .statusCode(200);

        await().atMost(5, SECONDS).until(() -> getSpans().size() == 1);
        assertThat(getSpans()).singleElement().satisfies(m -> {
            assertThat(m).extractingByKey("name").isEqualTo("GET /secured/item/{value}");
        });
    }
}
