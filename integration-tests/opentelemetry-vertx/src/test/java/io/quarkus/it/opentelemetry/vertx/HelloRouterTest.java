package io.quarkus.it.opentelemetry.vertx;

import static io.opentelemetry.api.trace.SpanKind.CONSUMER;
import static io.opentelemetry.api.trace.SpanKind.PRODUCER;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_STATUS_CODE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_DESTINATION;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_DESTINATION_KIND;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_OPERATION;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.MESSAGING_SYSTEM;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessageOperation;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
class HelloRouterTest {
    @BeforeEach
    @AfterEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 0);
    }

    @Test
    void span() {
        given()
                .get("/hello")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("hello"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 1);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(1, spans.size());

        assertEquals(SERVER.toString(), spans.get(0).get("kind"));
        assertEquals("/hello", spans.get(0).get("name"));
        assertEquals(HTTP_OK, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_STATUS_CODE.toString()));
        assertEquals(HttpMethod.GET.toString(), ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_METHOD.toString()));
        assertEquals("/hello", ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_ROUTE.toString()));
    }

    @Test
    void spanPath() {
        given()
                .get("/hello/{name}", "Naruto")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("hello Naruto"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 1);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(1, spans.size());

        assertEquals(SERVER.toString(), spans.get(0).get("kind"));
        assertEquals("/hello/:name", spans.get(0).get("name"));
        assertEquals(HTTP_OK, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_STATUS_CODE.toString()));
        assertEquals(HttpMethod.GET.toString(), ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_METHOD.toString()));
        assertEquals("/hello/:name", ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_ROUTE.toString()));
    }

    @Test
    void post() {
        given()
                .contentType(ContentType.TEXT)
                .body("Naruto")
                .post("/hello/")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("hello Naruto"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 1);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(1, spans.size());

        assertEquals(SERVER.toString(), spans.get(0).get("kind"));
        assertEquals("/hello", spans.get(0).get("name"));
        assertEquals(HTTP_OK, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_STATUS_CODE.toString()));
        assertEquals(HttpMethod.POST.toString(), ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_METHOD.toString()));
        assertEquals("/hello", ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_ROUTE.toString()));
        assertEquals(6, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_REQUEST_CONTENT_LENGTH.toString()));
        assertEquals(12, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_RESPONSE_CONTENT_LENGTH.toString()));
    }

    @Test
    void bus() {
        given()
                .get("/bus")
                .then()
                .statusCode(HTTP_OK)
                .body(equalTo("hello"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getMessages().size() == 1);

        List<String> messages = getMessages();
        assertEquals(1, messages.size());
        assertEquals("hello to bus", messages.get(0));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size());

        assertEquals(spans.get(0).get("traceId"), spans.get(1).get("traceId"));
        assertEquals(spans.get(1).get("traceId"), spans.get(2).get("traceId"));

        assertEquals(CONSUMER.toString(), spans.get(0).get("kind"));
        assertEquals("vert.x", ((Map<?, ?>) spans.get(0).get("attributes")).get(MESSAGING_SYSTEM.toString()));
        assertEquals("topic", ((Map<?, ?>) spans.get(0).get("attributes")).get(MESSAGING_DESTINATION_KIND.toString()));
        assertEquals("bus", ((Map<?, ?>) spans.get(0).get("attributes")).get(MESSAGING_DESTINATION.toString()));
        assertEquals(MessageOperation.RECEIVE.operationName(),
                ((Map<?, ?>) spans.get(0).get("attributes")).get(MESSAGING_OPERATION.toString()));

        assertEquals(PRODUCER.toString(), spans.get(1).get("kind"));
        assertEquals("vert.x", ((Map<?, ?>) spans.get(1).get("attributes")).get(MESSAGING_SYSTEM.toString()));
        assertEquals("topic", ((Map<?, ?>) spans.get(1).get("attributes")).get(MESSAGING_DESTINATION_KIND.toString()));
        assertEquals("bus", ((Map<?, ?>) spans.get(1).get("attributes")).get(MESSAGING_DESTINATION.toString()));

        assertEquals(SERVER.toString(), spans.get(2).get("kind"));
        assertEquals("/bus", spans.get(2).get("name"));
        assertEquals(HTTP_OK, ((Map<?, ?>) spans.get(2).get("attributes")).get(HTTP_STATUS_CODE.toString()));
        assertEquals(HttpMethod.GET.toString(), ((Map<?, ?>) spans.get(2).get("attributes")).get(HTTP_METHOD.toString()));
        assertEquals("/bus", ((Map<?, ?>) spans.get(2).get("attributes")).get(HTTP_ROUTE.toString()));
    }

    private static List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    private static List<String> getMessages() {
        return given().get("/bus/messages").body().as(new TypeRef<>() {
        });
    }
}
