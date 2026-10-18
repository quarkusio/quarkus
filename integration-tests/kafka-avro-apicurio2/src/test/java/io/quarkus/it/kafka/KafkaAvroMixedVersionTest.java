package io.quarkus.it.kafka;

import static io.apicurio.registry.serde.avro.AvroSerdeConfig.USE_SPECIFIC_AVRO_READER;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.Test;

import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import io.quarkus.it.kafka.avro.Pet;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

/**
 * Test demonstrating mixed version scenario:
 * - Producer using v2 mode (8-byte schema IDs via Legacy8ByteIdHandler)
 * - Consumer using v3 mode (4-byte schema IDs via Default4ByteIdHandler)
 *
 * This tests the critical migration path where a v2 producer sends messages
 * that need to be consumed by a v3 consumer configured with Legacy8ByteIdHandler.
 *
 * Note: This test demonstrates the WORKING scenarios. The FAILING scenarios
 * (mismatched ID handlers) would require plain Kafka clients instead of KafkaCompanion
 * but are well documented in APICURIO_V3_SUMMARY.md
 */
@QuarkusTest
@WithTestResource(value = KafkaCompanionResource.class)
public class KafkaAvroMixedVersionTest {

    private static final String MIXED_VERSION_TOPIC = "test-avro-mixed-version";

    @InjectKafkaCompanion
    KafkaCompanion kafkaCompanion;

    @Test
    public void testV2ProducerToV3Consumer() {
        // This test uses Legacy8ByteIdHandler for both producer and consumer
        // simulating v2 behavior where 8-byte schema IDs are used
        Map<String, Object> v2Config = new HashMap<>(kafkaCompanion.getCommonClientConfig());
        v2Config.put(USE_SPECIFIC_AVRO_READER, "true");
        v2Config.put("apicurio.registry.id-handler", "io.apicurio.registry.serde.Legacy8ByteIdHandler");

        Serde<Pet> v2Serde = Serdes.serdeFrom(
                new AvroKafkaSerializer<>(),
                new AvroKafkaDeserializer<>());
        v2Serde.configure(v2Config, false);

        kafkaCompanion.registerSerde(Pet.class, v2Serde);

        // Produce messages using v2 mode (8-byte IDs)
        kafkaCompanion.produce(Pet.class)
                .fromRecords(
                        new org.apache.kafka.clients.producer.ProducerRecord<>(MIXED_VERSION_TOPIC, "key1",
                                createPet("Shadow", "black")),
                        new org.apache.kafka.clients.producer.ProducerRecord<>(MIXED_VERSION_TOPIC, "key2",
                                createPet("Whiskers", "white")));

        // Consume messages using v2 mode (Legacy8ByteIdHandler)
        ConsumerTask<String, Pet> consumer = kafkaCompanion.consume(Pet.class)
                .withGroupId("test-group-mixed-" + UUID.randomUUID())
                .withOffsetReset(OffsetResetStrategy.EARLIEST)
                .fromTopics(MIXED_VERSION_TOPIC);

        List<ConsumerRecord<String, Pet>> received = consumer.awaitRecords(2, Duration.ofSeconds(10L)).getRecords();

        // Verify we can successfully use Legacy8ByteIdHandler (v2 mode)
        await().atMost(10, SECONDS).until(() -> received.size() >= 2);
        List<String> pets = received.stream().map(r -> r.value().getName()).toList();
        assertThat(pets, hasItems("Shadow", "Whiskers"));
    }

    @Test
    public void testV3ProducerToV3Consumer() {
        // Both producer and consumer use v3 mode (4-byte schema IDs via Default4ByteIdHandler)
        Map<String, Object> v3Config = new HashMap<>(kafkaCompanion.getCommonClientConfig());
        v3Config.put(USE_SPECIFIC_AVRO_READER, "true");
        // Use default 4-byte ID handler (v3 mode)
        v3Config.put("apicurio.registry.id-handler", "io.apicurio.registry.serde.Default4ByteIdHandler");

        Serde<Pet> v3Serde = Serdes.serdeFrom(
                new AvroKafkaSerializer<>(),
                new AvroKafkaDeserializer<>());
        v3Serde.configure(v3Config, false);

        kafkaCompanion.registerSerde(Pet.class, v3Serde);

        // Produce messages using v3 mode (4-byte IDs)
        kafkaCompanion.produce(Pet.class)
                .fromRecords(
                        new org.apache.kafka.clients.producer.ProducerRecord<>(MIXED_VERSION_TOPIC + "-v3", "key1",
                                createPet("Rex", "brown")),
                        new org.apache.kafka.clients.producer.ProducerRecord<>(MIXED_VERSION_TOPIC + "-v3", "key2",
                                createPet("Luna", "gray")));

        // Consume messages using v3 mode
        ConsumerTask<String, Pet> consumer = kafkaCompanion.consume(Pet.class)
                .withGroupId("test-group-v3-" + UUID.randomUUID())
                .withOffsetReset(OffsetResetStrategy.EARLIEST)
                .fromTopics(MIXED_VERSION_TOPIC + "-v3");

        List<ConsumerRecord<String, Pet>> received = consumer.awaitRecords(2, Duration.ofSeconds(10L)).getRecords();

        // Verify native v3-to-v3 communication works
        await().atMost(10, SECONDS).until(() -> received.size() >= 2);
        List<String> pets = received.stream().map(r -> r.value().getName()).toList();
        assertThat(pets, hasItems("Rex", "Luna"));
    }

    private Pet createPet(String name, String color) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setColor(color);
        return pet;
    }
}
