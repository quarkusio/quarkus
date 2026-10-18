package io.quarkus.kafka.client.runtime.dev.ui.model.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaMessageContent;

class KafkaMessageDecoderRegistryTest {

    @Test
    void testDecodeWithNoDecoders() {
        KafkaMessageDecoderRegistry registry = new KafkaMessageDecoderRegistry(List.of());

        KafkaMessageContent decoded = registry.decode("test-topic", "test-message".getBytes());

        assertEquals("test-message", decoded.value());
        assertEquals("STRING", decoded.format());
    }

    @Test
    void testDecodeWithDecoderThatCanNotDecode() {
        KafkaMessageDecoder decoder = new KafkaMessageDecoder() {
            @Override
            public boolean canDecode(byte[] data) {
                return false;
            }

            @Override
            public KafkaMessageContent decode(String topic, byte[] data) {
                return null;
            }
        };

        KafkaMessageDecoderRegistry registry = new KafkaMessageDecoderRegistry(List.of(decoder));

        KafkaMessageContent decoded = registry.decode("test-topic", "test-message".getBytes());

        assertEquals("test-message", decoded.value());
        assertEquals("STRING", decoded.format());
    }

    @Test
    void testDecodeWithDecoderThatCanDecode() {
        KafkaMessageDecoder decoder = new KafkaMessageDecoder() {
            @Override
            public boolean canDecode(byte[] data) {
                return true;
            }

            @Override
            public KafkaMessageContent decode(String topic, byte[] data) {
                return new KafkaMessageContent(new String(data, StandardCharsets.UTF_8), "MY_FORMAT");
            }
        };

        KafkaMessageDecoderRegistry registry = new KafkaMessageDecoderRegistry(List.of(decoder));

        KafkaMessageContent decoded = registry.decode("test-topic", "test-message".getBytes());

        assertEquals("test-message", decoded.value());
        assertEquals("MY_FORMAT", decoded.format());
    }

    @Test
    void testDecodeWithDecodersThatCanDecodeFirstShouldApply() {
        KafkaMessageDecoder decoder1 = new KafkaMessageDecoder() {
            @Override
            public boolean canDecode(byte[] data) {
                return true;
            }

            @Override
            public KafkaMessageContent decode(String topic, byte[] data) {
                return new KafkaMessageContent(new String(data, StandardCharsets.UTF_8), "MY_FORMAT_1");
            }
        };

        KafkaMessageDecoder decoder2 = new KafkaMessageDecoder() {
            @Override
            public boolean canDecode(byte[] data) {
                return true;
            }

            @Override
            public KafkaMessageContent decode(String topic, byte[] data) {
                return new KafkaMessageContent(new String(data, StandardCharsets.UTF_8), "MY_FORMAT_2");
            }
        };

        KafkaMessageDecoderRegistry registry = new KafkaMessageDecoderRegistry(List.of(decoder1, decoder2));

        KafkaMessageContent decoded = registry.decode("test-topic", "test-message".getBytes());

        assertEquals("test-message", decoded.value());
        assertEquals("MY_FORMAT_1", decoded.format());
    }
}
