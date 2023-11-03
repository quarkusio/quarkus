package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

@ApplicationScoped
public class DataForKeyedProducer {
    @Outgoing("data-for-keyed-out")
    public Multi<Message<String>> generateDataWithMetadata() {
        return Multi.createFrom().items(
                Message.of("a").addMetadata(OutgoingKafkaRecordMetadata.builder().withKey("a").build()),
                Message.of("b").addMetadata(OutgoingKafkaRecordMetadata.builder().withKey("b").build()),
                Message.of("c").addMetadata(OutgoingKafkaRecordMetadata.builder().withKey("a").build()));
    }
}
