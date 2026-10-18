package io.quarkus.kafka.client.runtime.dev.ui.model.converter;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.utils.Bytes;

import io.quarkus.kafka.client.runtime.dev.ui.model.decoder.KafkaMessageDecoderRegistry;
import io.quarkus.kafka.client.runtime.dev.ui.model.response.KafkaMessage;

@ApplicationScoped
public class KafkaModelConverter {

    @Inject
    KafkaMessageDecoderRegistry decoderRegistry;

    public KafkaMessage convert(ConsumerRecord<Bytes, Bytes> message) {
        return new KafkaMessage(
                message.topic(),
                message.partition(),
                message.offset(),
                message.timestamp(),
                Optional.ofNullable(message.key()).map((t) -> {
                    return new String(t.get(), StandardCharsets.UTF_8);
                }).orElse(null),
                Optional.ofNullable(message.value())
                        .map(Bytes::get)
                        .map(value -> decoderRegistry.decode(message.topic(), value))
                        .orElse(null),
                headers(message));
    }

    private static Map<String, String> headers(ConsumerRecord<Bytes, Bytes> message) {
        return StreamSupport.stream(message.headers().spliterator(), false)
                .collect(Collectors.toMap(Header::key, header -> new String(header.value(), StandardCharsets.UTF_8),
                        (existing, replacement) -> replacement));
    }
}
