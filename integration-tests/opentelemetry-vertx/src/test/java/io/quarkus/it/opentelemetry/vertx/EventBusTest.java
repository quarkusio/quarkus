package io.quarkus.it.opentelemetry.vertx;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;

@QuarkusTest
public class EventBusTest extends SpanExporterBaseTest {

    @BeforeEach
    void reset() {
        given().get("/reset").then().statusCode(HTTP_OK);
        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().isEmpty());
    }

    @Test
    public void testEventBusWithString() {
        String body = new JsonObject().put("name", "Bob Morane").toString();
        given().contentType(ContentType.JSON).body(body)
                .post("/event-bus/person")
                .then().statusCode(200).body(equalTo("Hello Bob Morane"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() >= 3);
        List<Map<String, Object>> spans = getSpans();

        Map<String, Object> serverCall = getSpanByKindAndParentId(spans, SpanKind.SERVER, SpanId.getInvalid());
        String spanId = getSpanId(serverCall);
        assertNotEquals(SpanId.getInvalid(), spanId);

        Map<String, Object> producerSpan = getSpanByKindAndParentId(spans, SpanKind.PRODUCER, spanId);
        String producerSpanId = getSpanId(producerSpan);
        assertNotEquals(SpanId.getInvalid(), producerSpanId);

        Map<String, Object> consumerSpan = getSpanByKindAndParentId(spans, SpanKind.CONSUMER, producerSpanId);
        String consumerSpanId = getSpanId(consumerSpan);
        assertNotEquals(SpanId.getInvalid(), consumerSpanId);

        Map<String, Object> methodCallSpan = getSpanByKindAndParentId(spans, SpanKind.INTERNAL, consumerSpanId);
        String methodCallSpanId = getSpanId(methodCallSpan);
        assertNotEquals(SpanId.getInvalid(), methodCallSpanId);
    }

    @Test
    public void testEventBusWithObjectAndHeader() {
        String body = new JsonObject()
                .put("firstName", "Bob")
                .put("lastName", "Morane")
                .toString();
        given().contentType(ContentType.JSON).body(body)
                .post("/event-bus/person2")
                .then().statusCode(200)
                // For some reason Multimap.toString() has \n at the end.
                .body(CoreMatchers.startsWith("Hello Bob Morane, header=headerValue\n"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() >= 3);
        List<Map<String, Object>> spans = getSpans();

        Map<String, Object> serverCall = getSpanByKindAndParentId(spans, SpanKind.SERVER, SpanId.getInvalid());
        String spanId = getSpanId(serverCall);
        assertNotEquals(SpanId.getInvalid(), spanId);

        Map<String, Object> producerSpan = getSpanByKindAndParentId(spans, SpanKind.PRODUCER, spanId);
        String producerSpanId = getSpanId(producerSpan);
        assertNotEquals(SpanId.getInvalid(), producerSpanId);

        Map<String, Object> consumerSpan = getSpanByKindAndParentId(spans, SpanKind.CONSUMER, producerSpanId);
        String consumerSpanId = getSpanId(consumerSpan);
        assertNotEquals(SpanId.getInvalid(), consumerSpanId);

        Map<String, Object> methodCallSpan = getSpanByKindAndParentId(spans, SpanKind.INTERNAL, consumerSpanId);
        String methodCallSpanId = getSpanId(methodCallSpan);
        assertNotEquals(SpanId.getInvalid(), methodCallSpanId);
    }

    @Test
    public void testEventBusWithPet() {
        String body = new JsonObject().put("name", "Neo").put("kind", "rabbit").toString();
        given().contentType(ContentType.JSON).body(body)
                .post("/event-bus/pet")
                .then().statusCode(200).body(equalTo("Hello Neo (rabbit)"));

        await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() >= 3);
        List<Map<String, Object>> spans = getSpans();

        Map<String, Object> serverCall = getSpanByKindAndParentId(spans, SpanKind.SERVER, SpanId.getInvalid());
        String spanId = getSpanId(serverCall);
        assertNotEquals(SpanId.getInvalid(), spanId);

        Map<String, Object> producerSpan = getSpanByKindAndParentId(spans, SpanKind.PRODUCER, spanId);
        String producerSpanId = getSpanId(producerSpan);
        assertNotEquals(SpanId.getInvalid(), producerSpanId);

        Map<String, Object> consumerSpan = getSpanByKindAndParentId(spans, SpanKind.CONSUMER, producerSpanId);
        String consumerSpanId = getSpanId(consumerSpan);
        assertNotEquals(SpanId.getInvalid(), consumerSpanId);

        Map<String, Object> methodCallSpan = getSpanByKindAndParentId(spans, SpanKind.INTERNAL, consumerSpanId);
        String methodCallSpanId = getSpanId(methodCallSpan);
        assertNotEquals(SpanId.getInvalid(), methodCallSpanId);
    }
}
