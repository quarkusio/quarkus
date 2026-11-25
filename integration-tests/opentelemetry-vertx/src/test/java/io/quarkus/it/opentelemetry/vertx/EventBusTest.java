package io.quarkus.it.opentelemetry.vertx;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URI;
import java.time.Duration;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;

@QuarkusTest
public class EventBusTest extends SpanExporterBaseTest {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @BeforeEach
    void reset() {
        spanExporter().reset();
    }

    @Test
    public void testEventBusWithString() {
        String body = new JsonObject().put("name", "Bob Morane").toString();
        given().contentType(ContentType.JSON).body(body)
                .post("/event-bus/person")
                .then().statusCode(200).body(equalTo("Hello Bob Morane"));

        SpanDataList spans = spanExporter().awaitCount(4, Duration.ofSeconds(5));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData serverCall = spans.ofKind(SpanKind.SERVER).roots().single();
        assertNotEquals(SpanId.getInvalid(), serverCall.getSpanId());

        SpanData producerSpan = spans.ofKind(SpanKind.PRODUCER).childrenOf(serverCall).single();
        assertNotEquals(SpanId.getInvalid(), producerSpan.getSpanId());

        SpanData consumerSpan = spans.ofKind(SpanKind.CONSUMER).childrenOf(producerSpan).single();
        assertNotEquals(SpanId.getInvalid(), consumerSpan.getSpanId());

        SpanData methodCallSpan = spans.ofKind(SpanKind.INTERNAL).childrenOf(consumerSpan).single();
        assertNotEquals(SpanId.getInvalid(), methodCallSpan.getSpanId());
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

        SpanDataList spans = spanExporter().awaitCount(4, Duration.ofSeconds(5));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData serverCall = spans.ofKind(SpanKind.SERVER).roots().single();
        assertNotEquals(SpanId.getInvalid(), serverCall.getSpanId());

        SpanData producerSpan = spans.ofKind(SpanKind.PRODUCER).childrenOf(serverCall).single();
        assertNotEquals(SpanId.getInvalid(), producerSpan.getSpanId());

        SpanData consumerSpan = spans.ofKind(SpanKind.CONSUMER).childrenOf(producerSpan).single();
        assertNotEquals(SpanId.getInvalid(), consumerSpan.getSpanId());

        SpanData methodCallSpan = spans.ofKind(SpanKind.INTERNAL).childrenOf(consumerSpan).single();
        assertNotEquals(SpanId.getInvalid(), methodCallSpan.getSpanId());
    }

    @Test
    public void testEventBusWithPet() {
        String body = new JsonObject().put("name", "Neo").put("kind", "rabbit").toString();
        given().contentType(ContentType.JSON).body(body)
                .post("/event-bus/pet")
                .then().statusCode(200).body(equalTo("Hello Neo (rabbit)"));

        SpanDataList spans = spanExporter().awaitCount(4, Duration.ofSeconds(5));
        TracesAssert.assertThat(spans).hasSize(1);

        SpanData serverCall = spans.ofKind(SpanKind.SERVER).roots().single();
        assertNotEquals(SpanId.getInvalid(), serverCall.getSpanId());

        SpanData producerSpan = spans.ofKind(SpanKind.PRODUCER).childrenOf(serverCall).single();
        assertNotEquals(SpanId.getInvalid(), producerSpan.getSpanId());

        SpanData consumerSpan = spans.ofKind(SpanKind.CONSUMER).childrenOf(producerSpan).single();
        assertNotEquals(SpanId.getInvalid(), consumerSpan.getSpanId());

        SpanData methodCallSpan = spans.ofKind(SpanKind.INTERNAL).childrenOf(consumerSpan).single();
        assertNotEquals(SpanId.getInvalid(), methodCallSpan.getSpanId());
    }
}
