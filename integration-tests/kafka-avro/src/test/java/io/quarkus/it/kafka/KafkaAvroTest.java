package io.quarkus.it.kafka;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.apicurio.registry.utils.serde.AbstractKafkaSerDe;
import io.apicurio.registry.utils.serde.AbstractKafkaSerializer;
import io.apicurio.registry.utils.serde.AvroKafkaDeserializer;
import io.apicurio.registry.utils.serde.AvroKafkaSerializer;
import io.apicurio.registry.utils.serde.avro.AvroDatumProvider;
import io.apicurio.registry.utils.serde.avro.DefaultAvroDatumProvider;
import io.apicurio.registry.utils.serde.strategy.GetOrCreateIdStrategy;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.quarkus.it.kafka.avro.Pet;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.RestAssured;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerBuilder;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.smallrye.reactive.messaging.kafka.companion.ProducerBuilder;

@QuarkusTest
@QuarkusTestResource(KafkaAndSchemaRegistryTestResource.class)
@QuarkusTestResource(KafkaCompanionResource.class)
public class KafkaAvroTest {

    private static final String CONFLUENT_PATH = "/avro/confluent";
    private static final String APICURIO_PATH = "/avro/apicurio";

    @InjectKafkaCompanion
    KafkaCompanion companion;

    @Test
    public void testConfluentAvroProducer() {
        configureConfluentSerde();
        ConsumerBuilder<Integer, Pet> consumer = companion.consume(Integer.class, Pet.class);
        ConsumerTask<Integer, Pet> consumeTask = consumer.withGroupId("test-avro-confluent")
                .fromTopics("test-avro-confluent-producer", 1);
        testAvroProducer(consumeTask, CONFLUENT_PATH);
    }

    @Test
    public void testConfluentAvroConsumer() {
        configureConfluentSerde();
        ProducerBuilder<Integer, Pet> producer = companion.produce(Integer.class, Pet.class);
        producer.withClientId("test-avro-confluent-test");
        testAvroConsumer(producer, CONFLUENT_PATH, "test-avro-confluent-consumer");
    }

    @Test
    public void testApicurioAvroProducer() {
        configureApicurioSerde();
        ConsumerBuilder<Integer, Pet> consumer = companion.consume(Integer.class, Pet.class);
        ConsumerTask<Integer, Pet> consumeTask = consumer.withGroupId("test-avro-apicurio")
                .fromTopics("test-avro-apicurio-producer", 1);
        testAvroProducer(consumeTask, APICURIO_PATH);
    }

    @Test
    public void testApicurioAvroConsumer() {
        configureApicurioSerde();
        ProducerBuilder<Integer, Pet> producer = companion.produce(Integer.class, Pet.class);
        producer.withClientId("test-avro-apicurio-test")
                .withProp(ProducerConfig.ACKS_CONFIG, "all");
        testAvroConsumer(producer, APICURIO_PATH, "test-avro-apicurio-consumer");
    }

    private void testAvroProducer(ConsumerTask<Integer, Pet> consumeTask, String path) {
        RestAssured.given()
                .header("content-type", "application/json")
                .body("{\"name\":\"neo\", \"color\":\"tricolor\"}")
                .post(path);
        ConsumerRecord<Integer, Pet> records = consumeTask.awaitCompletion().getFirstRecord();
        Assertions.assertEquals(records.key(), (Integer) 0);
        Pet pet = records.value();
        Assertions.assertEquals("neo", pet.getName());
        Assertions.assertEquals("tricolor", pet.getColor());
    }

    private void testAvroConsumer(ProducerBuilder<Integer, Pet> producer, String path, String topic) {
        producer.fromRecords(new ProducerRecord<>(topic, 1, createPet())).awaitCompletion();
        Pet retrieved = RestAssured.when().get(path).as(Pet.class);
        Assertions.assertEquals("neo", retrieved.getName());
        Assertions.assertEquals("white", retrieved.getColor());
    }

    private Pet createPet() {
        Pet pet = new Pet();
        pet.setName("neo");
        pet.setColor("white");
        return pet;
    }

    private void configureConfluentSerde() {
        Serde<Pet> petSerde = serdeFrom(new KafkaAvroSerializer(), new KafkaAvroDeserializer());
        petSerde.configure(Map.of(
                SCHEMA_REGISTRY_URL_CONFIG, KafkaAndSchemaRegistryTestResource.getConfluentSchemaRegistryUrl(),
                SPECIFIC_AVRO_READER_CONFIG, "true"), false);
        companion.registerSerde(Pet.class, petSerde);
    }

    private void configureApicurioSerde() {
        Serde<Pet> petSerde = Serdes.serdeFrom(new AvroKafkaSerializer<>(), new AvroKafkaDeserializer<>());
        petSerde.configure(Map.of(
                AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM, KafkaAndSchemaRegistryTestResource.getApicurioSchemaRegistryUrl(),
                // procuder
                // this is a workaround for Apicurio Registry 1.2.2.Final bug: if `avro-datum-provider`
                // isn't set to `DefaultAvroDatumProvider` explicitly, `use-specific-avro-reader` isn't processed
                AvroDatumProvider.REGISTRY_USE_SPECIFIC_AVRO_READER_CONFIG_PARAM, "true",
                AvroDatumProvider.REGISTRY_AVRO_DATUM_PROVIDER_CONFIG_PARAM, DefaultAvroDatumProvider.class.getName(),
                // consumer
                AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM, GetOrCreateIdStrategy.class.getName()),
                false);
        companion.registerSerde(Pet.class, petSerde);
    }

    @SuppressWarnings({ "rawtypes" })
    <T> Serde<T> serdeFrom(Serializer serializer, Deserializer deserializer) {
        return (Serde<T>) Serdes.serdeFrom(serializer, deserializer);
    }
}
