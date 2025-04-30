package io.quarkus.it.opentelemetry.vertx;

import static io.opentelemetry.api.trace.SpanKind.CONSUMER;
import static io.opentelemetry.api.trace.SpanKind.PRODUCER;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_REQUEST_BODY_SIZE;
import static io.opentelemetry.semconv.SemanticAttributes.HTTP_RESPONSE_BODY_SIZE;
import static io.opentelemetry.semconv.SemanticAttributes.MESSAGING_DESTINATION_NAME;
import static io.opentelemetry.semconv.SemanticAttributes.MESSAGING_OPERATION;
import static io.opentelemetry.semconv.SemanticAttributes.MESSAGING_SYSTEM;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.toSet;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.core.http.HttpMethod;

@QuarkusTest
class HelloRouterTest extends SpanExporterBaseTest {

    @BeforeEach
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
        assertEquals("GET /hello", spans.get(0).get("name"));
        assertEquals(HTTP_OK, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_RESPONSE_STATUS_CODE.toString()));
        assertEquals(HttpMethod.GET.toString(),
                ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_REQUEST_METHOD.toString()));
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
        assertEquals("GET /hello/:name", spans.get(0).get("name"));
        assertEquals(HTTP_OK, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_RESPONSE_STATUS_CODE.toString()));
        assertEquals(HttpMethod.GET.toString(),
                ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_REQUEST_METHOD.toString()));
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

        await().atMost(5, TimeUnit.SECONDS).until(() -> spanSize(1));
        List<Map<String, Object>> spans = getSpans();
        assertEquals(1, spans.size());

        assertEquals(SERVER.toString(), spans.get(0).get("kind"));
        assertEquals("POST /hello", spans.get(0).get("name"));
        assertEquals(HTTP_OK, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_RESPONSE_STATUS_CODE.toString()));
        assertEquals(HttpMethod.POST.toString(),
                ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_REQUEST_METHOD.toString()));
        assertEquals("/hello", ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_ROUTE.toString()));
        assertEquals(6, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_REQUEST_BODY_SIZE.toString()));
        assertEquals(12, ((Map<?, ?>) spans.get(0).get("attributes")).get(HTTP_RESPONSE_BODY_SIZE.toString()));
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

        await().atMost(50, TimeUnit.SECONDS).until(() -> spanSize(3));
        List<Map<String, Object>> spans = getSpans();
        assertEquals(3, spans.size(), "found:" + printSpans(spans));
        assertEquals(1, spans.stream().map(map -> map.get("traceId")).collect(toSet()).size());

        Map<String, Object> server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals(SERVER.toString(), server.get("kind"));
        assertEquals(HTTP_OK, ((Map<?, ?>) server.get("attributes")).get(HTTP_RESPONSE_STATUS_CODE.toString()));
        assertEquals(HttpMethod.GET.toString(), ((Map<?, ?>) server.get("attributes")).get(HTTP_REQUEST_METHOD.toString()));
        assertEquals("/bus", ((Map<?, ?>) server.get("attributes")).get(HTTP_ROUTE.toString()));

        Map<String, Object> producer = getSpanByKindAndParentId(spans, PRODUCER, server.get("spanId"));
        assertEquals(PRODUCER.toString(), producer.get("kind"));
        assertEquals("vert.x", ((Map<?, ?>) producer.get("attributes")).get(MESSAGING_SYSTEM.toString()));
        assertEquals("bus", ((Map<?, ?>) producer.get("attributes")).get(MESSAGING_DESTINATION_NAME.toString()));
        assertEquals(producer.get("parentSpanId"), server.get("spanId"));

        Map<String, Object> consumer = getSpanByKindAndParentId(spans, CONSUMER, producer.get("spanId"));
        assertEquals(CONSUMER.toString(), consumer.get("kind"));
        assertEquals("vert.x", ((Map<?, ?>) consumer.get("attributes")).get(MESSAGING_SYSTEM.toString()));
        assertEquals("bus", ((Map<?, ?>) consumer.get("attributes")).get(MESSAGING_DESTINATION_NAME.toString()));
        assertEquals(MessageOperation.RECEIVE.toString().toLowerCase(Locale.ROOT),
                ((Map<?, ?>) consumer.get("attributes")).get(MESSAGING_OPERATION.toString()));
        assertEquals(consumer.get("parentSpanId"), producer.get("spanId"));
    }

}
