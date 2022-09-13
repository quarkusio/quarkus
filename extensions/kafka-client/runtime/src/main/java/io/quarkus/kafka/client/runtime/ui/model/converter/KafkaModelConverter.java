package io.quarkus.kafka.client.runtime.ui.model.converter;

import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.utils.Bytes;

import io.quarkus.kafka.client.runtime.ui.model.response.KafkaMessage;

public class KafkaModelConverter {
    public KafkaMessage convert(ConsumerRecord<Bytes, Bytes> message) {
        return new KafkaMessage(
                message.topic(),
                message.partition(),
                message.offset(),
                message.timestamp(),
                Optional.ofNullable(message.key()).map(Bytes::toString).orElse(null),
                Optional.ofNullable(message.value()).map(Bytes::toString).orElse(null));
    }
}
