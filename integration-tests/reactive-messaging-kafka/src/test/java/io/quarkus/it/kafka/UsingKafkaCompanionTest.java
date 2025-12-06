package io.quarkus.it.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

@QuarkusTest
@WithTestResource(KafkaCompanionResource.class)
@DisabledOnOs({ OS.WINDOWS }) // Kafka requires docker to start
@Tag("https://github.com/quarkusio/quarkus/issues/50751")
@Order(2)
class UsingKafkaCompanionTest {

    @Inject
    MessageEmitter messageTransmitter;

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Test
    void dashedTopic() {
        String message = "Hello, Quarkus!";
        messageTransmitter.emit(message);

        List<String> actual = companion.consumeStrings()
                .fromTopics("foo.bar-topic")
                .awaitRecords(1, Duration.ofSeconds(30))
                .stream()
                .map(ConsumerRecord::value)
                .toList();
        assertEquals(1, actual.size());
        assertEquals(message, actual.get(0));
    }
}
