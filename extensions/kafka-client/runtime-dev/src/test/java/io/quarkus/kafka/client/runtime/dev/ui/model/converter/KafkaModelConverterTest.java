package io.quarkus.kafka.client.runtime.dev.ui.model.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.utils.Bytes;
import org.junit.jupiter.api.Test;

import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaMessage;

class KafkaModelConverterTest {

    @Test
    void testConvertWithDuplicateHeaders() {
        // Given a Kafka message with duplicate headers
        RecordHeaders headers = new RecordHeaders();
        addHeader(headers, "duplicate-key", "value1");
        addHeader(headers, "duplicate-key", "value2");
        addHeader(headers, "unique-key", "value3");

        ConsumerRecord<Bytes, Bytes> record = new ConsumerRecord<>(
                "test-topic",
                0,
                100L,
                System.currentTimeMillis(),
                TimestampType.CREATE_TIME,
                0,
                0,
                Bytes.wrap("test-key".getBytes(StandardCharsets.UTF_8)),
                Bytes.wrap("test-value".getBytes(StandardCharsets.UTF_8)),
                headers,
                Optional.empty());

        KafkaModelConverter converter = new KafkaModelConverter();

        // When converting the record
        KafkaMessage message = converter.convert(record);

        // Then the conversion should succeed without throwing an exception
        assertNotNull(message);
        assertEquals("test-topic", message.getTopic());
        assertEquals(0, message.getPartition());
        assertEquals(100L, message.getOffset());
        assertEquals("test-key", message.getKey());
        assertEquals("test-value", message.getValue());

        // Headers should contain the last value for duplicate keys
        Map<String, String> convertedHeaders = message.getHeaders();
        assertNotNull(convertedHeaders);
        assertEquals(2, convertedHeaders.size());
        assertEquals("value2", convertedHeaders.get("duplicate-key"));
        assertEquals("value3", convertedHeaders.get("unique-key"));
    }

    @Test
    void testConvertWithUniqueHeaders() {
        // Given a Kafka message with unique headers
        RecordHeaders headers = new RecordHeaders();
        addHeader(headers, "header1", "value1");
        addHeader(headers, "header2", "value2");

        ConsumerRecord<Bytes, Bytes> record = new ConsumerRecord<>(
                "test-topic",
                1,
                200L,
                System.currentTimeMillis(),
                TimestampType.CREATE_TIME,
                0,
                0,
                Bytes.wrap("test-key".getBytes(StandardCharsets.UTF_8)),
                Bytes.wrap("test-value".getBytes(StandardCharsets.UTF_8)),
                headers,
                Optional.empty());

        KafkaModelConverter converter = new KafkaModelConverter();

        // When converting the record
        KafkaMessage message = converter.convert(record);

        // Then the conversion should succeed
        assertNotNull(message);
        Map<String, String> convertedHeaders = message.getHeaders();
        assertNotNull(convertedHeaders);
        assertEquals(2, convertedHeaders.size());
        assertEquals("value1", convertedHeaders.get("header1"));
        assertEquals("value2", convertedHeaders.get("header2"));
    }

    @Test
    void testConvertWithNoHeaders() {
        // Given a Kafka message with no headers
        RecordHeaders headers = new RecordHeaders();

        ConsumerRecord<Bytes, Bytes> record = new ConsumerRecord<>(
                "test-topic",
                2,
                300L,
                System.currentTimeMillis(),
                TimestampType.CREATE_TIME,
                0,
                0,
                Bytes.wrap("test-key".getBytes(StandardCharsets.UTF_8)),
                Bytes.wrap("test-value".getBytes(StandardCharsets.UTF_8)),
                headers,
                Optional.empty());

        KafkaModelConverter converter = new KafkaModelConverter();

        // When converting the record
        KafkaMessage message = converter.convert(record);

        // Then the conversion should succeed
        assertNotNull(message);
        Map<String, String> convertedHeaders = message.getHeaders();
        assertNotNull(convertedHeaders);
        assertEquals(0, convertedHeaders.size());
    }

    private static void addHeader(Headers headers, String key, String value1) {
        headers.add(key, value1.getBytes(StandardCharsets.UTF_8));
    }
}
