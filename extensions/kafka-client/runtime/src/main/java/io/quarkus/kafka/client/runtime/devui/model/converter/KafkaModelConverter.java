package io.quarkus.kafka.client.runtime.devui.model.converter;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.utils.Bytes;

import io.quarkus.kafka.client.runtime.devui.model.response.KafkaMessage;

public class KafkaModelConverter {
    public KafkaMessage convert(ConsumerRecord<Bytes, Bytes> message) {
        return new KafkaMessage(
                message.topic(),
                message.partition(),
                message.offset(),
                message.timestamp(),
                Optional.ofNullable(message.key()).map((t) -> {
                    return new String(t.get(), StandardCharsets.UTF_8);
                }).orElse(null),
                Optional.ofNullable(message.value()).map((t) -> {
                    return new String(t.get(), StandardCharsets.UTF_8);
                }).orElse(null),
                headers(message));
    }

    private static Map<String, String> headers(ConsumerRecord<Bytes, Bytes> message) {
        return StreamSupport.stream(message.headers().spliterator(), false)
                .collect(Collectors.toMap(Header::key, header -> new String(header.value(), StandardCharsets.UTF_8)));
    }
}
