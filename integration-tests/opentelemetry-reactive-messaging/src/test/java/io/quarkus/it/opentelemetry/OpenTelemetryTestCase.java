package io.quarkus.it.opentelemetry;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation.PUBLISH;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.ClientAttributes.CLIENT_ADDRESS;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_LANGUAGE;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_NAME;
import static io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_VERSION;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_SCHEME;
import static io.opentelemetry.semconv.UserAgentAttributes.USER_AGENT_ORIGINAL;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_CLIENT_ID;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_CONSUMER_GROUP_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_PARTITION_ID;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_SYSTEM;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.quarkus.test.opentelemetry.SpanDataList;
import io.quarkus.test.opentelemetry.SpanExporterClient;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
public class OpenTelemetryTestCase {

    @TestHTTPResource
    URI applicationUri;

    SpanExporterClient spanExporter() {
        return SpanExporterClient.at(applicationUri);
    }

    @TestHTTPResource("direct")
    URL directUrl;

    @TestHTTPResource("uni")
    URL uniUrl;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @BeforeEach
    @AfterEach
    void reset() {
        spanExporter().reset();
    }

    @Test
    void testProducerConsumerTracing() {
        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        SpanDataList spans = spanExporter().awaitCount(4, Duration.ofMinutes(1));

        SpanData serverSpan = spans.ofKind(SpanKind.SERVER).single();
        verifyServer(serverSpan, null, directUrl);

        SpanData producerSpan = spans.ofKind(SpanKind.PRODUCER).single();
        verifyProducer(producerSpan, serverSpan, "traces");

        SpanData consumerSpan = spans.ofKind(SpanKind.CONSUMER).single();
        verifyConsumer(consumerSpan, producerSpan, true, "traces", "traces-in");

        SpanData cdiSpan = spans.ofKind(SpanKind.INTERNAL).single();
        verifyCdiCall(cdiSpan, consumerSpan);
    }

    @Test
    void testProcessorTracing() {
        companion.produceStrings().fromRecords(new ProducerRecord<>("traces2", "1"));

        SpanDataList spans = spanExporter().awaitCount(3, Duration.ofMinutes(1));

        SpanData consumerSpan = spans.ofKind(SpanKind.CONSUMER).single();
        verifyConsumer(consumerSpan, null, false, "traces2", "traces-in2");

        SpanData cdiSpan = spans.ofKind(SpanKind.INTERNAL).single();
        verifyCdiCall(cdiSpan, consumerSpan);

        SpanData producerSpan = spans.ofKind(SpanKind.PRODUCER).single();
        verifyProducer(producerSpan, consumerSpan, "traces-processed");
    }

    /**
     * Otel Context was propagated regardless of "@CurrentThreadContext(propagated = {})"
     */
    @Test
    void testUniContextCleared() {
        given().body("rose").post("/uni").then()
                .statusCode(204);

        SpanDataList spans = spanExporter().awaitCount(4, Duration.ofMinutes(1));

        SpanData serverSpan = spans.ofKind(SpanKind.SERVER).single();
        Assertions.assertNotNull(serverSpan.getSpanId());
        verifyResource(serverSpan, null);

        assertThat(serverSpan).hasName("POST /uni");
        Assertions.assertTrue(serverSpan.hasEnded());
        Assertions.assertFalse(serverSpan.getParentSpanContext().isRemote());

        assertThat(serverSpan)
                .hasAttribute(HTTP_REQUEST_METHOD, "POST")
                .hasAttribute(URL_PATH, "/uni")
                .hasAttribute(SERVER_ADDRESS, uniUrl.getHost())
                .hasAttribute(SERVER_PORT, (long) uniUrl.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 204L);
        Assertions.assertNotNull(serverSpan.getAttributes().get(CLIENT_ADDRESS));
        Assertions.assertNotNull(serverSpan.getAttributes().get(USER_AGENT_ORIGINAL));

        // Producer OTel context was cleared by using @CurrentThreadContext(propagated = {}). No parent.
        SpanData producerSpan = spans.ofKind(SpanKind.PRODUCER).single();
        verifyProducer(producerSpan, null, "traces");

        SpanData consumerSpan = spans.ofKind(SpanKind.CONSUMER).single();
        verifyConsumer(consumerSpan, producerSpan, true, "traces", "traces-in");

        SpanData internalSpan = spans.ofKind(SpanKind.INTERNAL).single();
        verifyInternal(internalSpan, consumerSpan);
    }

    private static void verifyInternal(SpanData internalSpan, SpanData consumerSpan) {
        assertThat(internalSpan).hasName("TracedService.call");
        assertEquals(consumerSpan.getSpanId(), internalSpan.getParentSpanContext().getSpanId());
    }

    private void verifyServer(SpanData span, SpanData parentSpan, URL url) {
        Assertions.assertNotNull(span.getSpanId());
        verifyResource(span, parentSpan);

        assertThat(span)
                .hasName("GET /direct")
                .hasKind(SpanKind.SERVER);
        Assertions.assertTrue(span.hasEnded());
        Assertions.assertFalse(span.getParentSpanContext().isRemote());

        assertThat(span)
                .hasAttribute(HTTP_REQUEST_METHOD, "GET")
                .hasAttribute(URL_PATH, "/direct")
                .hasAttribute(SERVER_ADDRESS, url.getHost())
                .hasAttribute(SERVER_PORT, (long) url.getPort())
                .hasAttribute(URL_SCHEME, "http")
                .hasAttribute(HTTP_RESPONSE_STATUS_CODE, 200L);
        Assertions.assertNotNull(span.getAttributes().get(CLIENT_ADDRESS));
        Assertions.assertNotNull(span.getAttributes().get(USER_AGENT_ORIGINAL));
    }

    private static void verifyProducer(SpanData span,
            SpanData parentSpan,
            String topic) {
        Assertions.assertNotNull(span.getSpanId());
        verifyResource(span, parentSpan);

        assertThat(span)
                .hasName(topic + " " + PUBLISH.name().toLowerCase())
                .hasKind(SpanKind.PRODUCER);
        Assertions.assertTrue(span.hasEnded());
        Assertions.assertFalse(span.getParentSpanContext().isRemote());

        assertThat(span)
                .hasAttribute(MESSAGING_SYSTEM, "kafka")
                .hasAttribute(MESSAGING_DESTINATION_NAME, topic);
    }

    private static void verifyConsumer(SpanData span,
            SpanData parentSpan,
            boolean parentRemote,
            String topic,
            String channel) {
        Assertions.assertNotNull(span.getSpanId());
        verifyResource(span, parentSpan);

        assertThat(span)
                .hasName(topic + " receive")
                .hasKind(SpanKind.CONSUMER);
        Assertions.assertTrue(span.hasEnded());
        Assertions.assertEquals(parentRemote, span.getParentSpanContext().isRemote());

        assertThat(span)
                .hasAttribute(MESSAGING_CLIENT_ID, "opentelemetry-integration-test - kafka-consumer-" + channel)
                .hasAttribute(MESSAGING_SYSTEM, "kafka")
                .hasAttribute(MESSAGING_DESTINATION_NAME, topic)
                .hasAttribute(MESSAGING_CONSUMER_GROUP_NAME, "opentelemetry-integration-test")
                .hasAttribute(MESSAGING_DESTINATION_PARTITION_ID, "0")
                .hasAttribute(stringKey("messaging.client_id"), "kafka-consumer-" + channel);
    }

    private void verifyCdiCall(SpanData span, SpanData parentSpan) {
        assertThat(span)
                .hasName("TracedService.call")
                .hasKind(SpanKind.INTERNAL);
        verifyResource(span, parentSpan);
    }

    private static void verifyResource(SpanData span, SpanData parentSpan) {
        Assertions.assertEquals("opentelemetry-integration-test", span.getResource().getAttribute(SERVICE_NAME));
        Assertions.assertEquals("999-SNAPSHOT", span.getResource().getAttribute(SERVICE_VERSION));
        Assertions.assertEquals("java", span.getResource().getAttribute(TELEMETRY_SDK_LANGUAGE));
        Assertions.assertEquals("opentelemetry", span.getResource().getAttribute(TELEMETRY_SDK_NAME));
        Assertions.assertNotNull(span.getResource().getAttribute(TELEMETRY_SDK_VERSION));
        String parentSpanId = SpanId.getInvalid();
        if (parentSpan != null) {
            parentSpanId = parentSpan.getSpanId();
            Assertions.assertEquals(parentSpan.getTraceId(), span.getParentSpanContext().getTraceId());
            // a span is in the trace of its parent
            assertThat(span).hasTraceId(parentSpan.getTraceId()).hasParent(parentSpan);
        } else {
            assertThat(span).hasNoParent();
        }
        Assertions.assertEquals(parentSpanId, span.getParentSpanContext().getSpanId());
        Assertions.assertEquals(!SpanId.getInvalid().equals(parentSpanId), span.getParentSpanContext().isValid());

    }

}
