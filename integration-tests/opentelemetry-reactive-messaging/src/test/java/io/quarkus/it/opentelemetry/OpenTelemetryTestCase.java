package io.quarkus.it.opentelemetry;

import static io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation.PUBLISH;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceId;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.common.mapper.TypeRef;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource.class)
public class OpenTelemetryTestCase {
    @TestHTTPResource("direct")
    URL directUrl;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    private void resetExporter() {
        given()
                .when().get("/export/clear")
                .then()
                .statusCode(204);
        await().atMost(5, SECONDS).until(() -> getSpans().size() == 0);
    }

    private List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {
        });
    }

    @NotNull
    private static Map<String, Object> findSpan(List<Map<String, Object>> spans,
            Predicate<Map<String, Object>> spanDataSelector) {
        Optional<Map<String, Object>> select = spans.stream().filter(spanDataSelector).findFirst();
        Assertions.assertTrue(select.isPresent());
        Map<String, Object> spanData = select.get();
        Assertions.assertNotNull(spanData.get("spanId"));
        return spanData;
    }

    @Test
    void testProducerConsumerTracing() {
        resetExporter();

        given()
                .contentType("application/json")
                .when().get("/direct")
                .then()
                .statusCode(200)
                .body("message", equalTo("Direct trace"));

        Awaitility.await().atMost(Duration.ofMinutes(1)).until(() -> getSpans().size() == 4);
        List<Map<String, Object>> spans = getSpans();

        Map<String, Object> serverSpan = findSpan(spans, m -> SpanKind.SERVER.name().equals(m.get("kind")));
        verifyServer(serverSpan, null, directUrl);

        Map<String, Object> producerSpan = findSpan(spans, m -> SpanKind.PRODUCER.name().equals(m.get("kind")));
        verifyProducer(producerSpan, serverSpan, "traces");

        Map<String, Object> consumerSpan = findSpan(spans, m -> SpanKind.CONSUMER.name().equals(m.get("kind")));
        verifyConsumer(consumerSpan, producerSpan, true, "traces", "traces-in");

        Map<String, Object> cdiSpan = findSpan(spans, m -> SpanKind.INTERNAL.name().equals(m.get("kind")));
        verifyCdiCall(cdiSpan, consumerSpan);
    }

    @Test
    void testProcessorTracing() {
        resetExporter();

        companion.produceStrings().fromRecords(new ProducerRecord<>("traces2", "1"));

        Awaitility.await().atMost(Duration.ofMinutes(1)).until(() -> getSpans().size() == 3);
        List<Map<String, Object>> spans = getSpans();

        Map<String, Object> consumerSpan = findSpan(spans, m -> SpanKind.CONSUMER.name().equals(m.get("kind")));
        verifyConsumer(consumerSpan, null, false, "traces2", "traces-in2");

        Map<String, Object> cdiSpan = findSpan(spans, m -> SpanKind.INTERNAL.name().equals(m.get("kind")));
        verifyCdiCall(cdiSpan, consumerSpan);

        Map<String, Object> producerSpan = findSpan(spans, m -> SpanKind.PRODUCER.name().equals(m.get("kind")));
        verifyProducer(producerSpan, consumerSpan, "traces-processed");
    }

    private void verifyServer(Map<String, Object> spanData, Map<String, Object> parentSpanData, URL url) {
        Assertions.assertNotNull(spanData.get("spanId"));
        verifyResource(spanData, parentSpanData);

        Assertions.assertEquals("GET /direct", spanData.get("name"));
        Assertions.assertEquals(SpanKind.SERVER.toString(), spanData.get("kind"));
        Assertions.assertTrue((Boolean) spanData.get("ended"));
        Assertions.assertFalse((Boolean) spanData.get("parent_remote"));

        Assertions.assertEquals("GET", spanData.get("attr_http.request.method"));
        Assertions.assertEquals("/direct", spanData.get("attr_url.path"));
        assertEquals(url.getHost(), spanData.get("attr_server.address"));
        assertEquals(url.getPort(), Integer.valueOf((String) spanData.get("attr_server.port")));
        Assertions.assertEquals("http", spanData.get("attr_url.scheme"));
        Assertions.assertEquals("200", spanData.get("attr_http.response.status_code"));
        Assertions.assertNotNull(spanData.get("attr_client.address"));
        Assertions.assertNotNull(spanData.get("attr_user_agent.original"));
    }

    private static void verifyProducer(Map<String, Object> spanData,
            Map<String, Object> parentSpanData,
            String topic) {
        Assertions.assertNotNull(spanData.get("spanId"));
        verifyResource(spanData, parentSpanData);

        Assertions.assertEquals(topic + " " + PUBLISH.name().toLowerCase(), spanData.get("name"));
        Assertions.assertEquals(SpanKind.PRODUCER.toString(), spanData.get("kind"));
        Assertions.assertTrue((Boolean) spanData.get("ended"));
        Assertions.assertFalse((Boolean) spanData.get("parent_remote"));

        Assertions.assertEquals("kafka", spanData.get("attr_messaging.system"));
        Assertions.assertEquals(topic, spanData.get("attr_messaging.destination.name"));
    }

    private static void verifyConsumer(Map<String, Object> spanData,
            Map<String, Object> parentSpanData,
            boolean parentRemote,
            String topic,
            String channel) {
        Assertions.assertNotNull(spanData.get("spanId"));
        verifyResource(spanData, parentSpanData);

        Assertions.assertEquals(topic + " receive", spanData.get("name"));
        Assertions.assertEquals(SpanKind.CONSUMER.toString(), spanData.get("kind"));
        Assertions.assertTrue((Boolean) spanData.get("ended"));
        Assertions.assertEquals(parentRemote, spanData.get("parent_remote"));

        Assertions.assertEquals("opentelemetry-integration-test - kafka-consumer-" + channel,
                spanData.get("attr_messaging.consumer.id"));
        Assertions.assertEquals("kafka", spanData.get("attr_messaging.system"));
        Assertions.assertEquals(topic, spanData.get("attr_messaging.destination.name"));
        Assertions.assertEquals("opentelemetry-integration-test", spanData.get("attr_messaging.kafka.consumer.group"));
        Assertions.assertEquals("0", spanData.get("attr_messaging.kafka.partition"));
        Assertions.assertEquals("kafka-consumer-" + channel, spanData.get("attr_messaging.client_id"));
        Assertions.assertEquals("0", spanData.get("attr_messaging.kafka.message.offset"));
    }

    private void verifyCdiCall(Map<String, Object> spanData, Map<String, Object> parentSpanData) {
        Assertions.assertEquals("TracedService.call", spanData.get("name"));
        Assertions.assertEquals(SpanKind.INTERNAL.toString(), spanData.get("kind"));
        verifyResource(spanData, parentSpanData);
    }

    private static void verifyResource(Map<String, Object> spanData, Map<String, Object> parentSpanData) {
        Assertions.assertEquals("opentelemetry-integration-test", spanData.get("resource_service.name"));
        Assertions.assertEquals("999-SNAPSHOT", spanData.get("resource_service.version"));
        Assertions.assertEquals("java", spanData.get("resource_telemetry.sdk.language"));
        Assertions.assertEquals("opentelemetry", spanData.get("resource_telemetry.sdk.name"));
        Assertions.assertNotNull(spanData.get("resource_telemetry.sdk.version"));
        String parentSpanId = SpanId.getInvalid();
        String parentTraceId = TraceId.getInvalid();
        if (parentSpanData != null) {
            parentSpanId = (String) parentSpanData.get("spanId");
            parentTraceId = (String) parentSpanData.get("traceId");
        }
        Assertions.assertEquals(parentSpanId, spanData.get("parent_spanId"));
        Assertions.assertEquals(parentTraceId, spanData.get("parent_traceId"));
        Assertions.assertEquals(!SpanId.getInvalid().equals(parentSpanId), spanData.get("parent_valid"));

    }

}
