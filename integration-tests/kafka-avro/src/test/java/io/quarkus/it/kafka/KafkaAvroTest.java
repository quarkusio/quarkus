package io.quarkus.it.kafka;

import java.time.Duration;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.kafka.avro.AvroKafkaCreator;
import io.quarkus.it.kafka.avro.Pet;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(KafkaAndSchemaRegistryTestResource.class)
public class KafkaAvroTest {

    private static final String CONFLUENT_PATH = "/avro/confluent";
    private static final String APICURIO_PATH = "/avro/apicurio";

    @Test
    public void testConfluentAvroProducer() {
        KafkaConsumer<Integer, Pet> consumer = AvroKafkaCreator.createConfluentConsumer(
                KafkaAndSchemaRegistryTestResource.getBootstrapServers(),
                KafkaAndSchemaRegistryTestResource.getConfluentSchemaRegistryUrl(),
                "test-avro-confluent",
                "test-avro-confluent-producer");
        testAvroProducer(consumer, CONFLUENT_PATH);
    }

    @Test
    public void testConfluentAvroConsumer() {
        KafkaProducer<Integer, Pet> producer = AvroKafkaCreator.createConfluentProducer(
                KafkaAndSchemaRegistryTestResource.getBootstrapServers(),
                KafkaAndSchemaRegistryTestResource.getConfluentSchemaRegistryUrl(),
                "test-avro-confluent-test");
        testAvroConsumer(producer, CONFLUENT_PATH, "test-avro-confluent-consumer");
    }

    @Test
    public void testApicurioAvroProducer() {
        KafkaConsumer<Integer, Pet> consumer = AvroKafkaCreator.createApicurioConsumer(
                KafkaAndSchemaRegistryTestResource.getBootstrapServers(),
                KafkaAndSchemaRegistryTestResource.getApicurioSchemaRegistryUrl(),
                "test-avro-apicurio",
                "test-avro-apicurio-producer");
        testAvroProducer(consumer, APICURIO_PATH);
    }

    @Test
    public void testApicurioAvroConsumer() {
        KafkaProducer<Integer, Pet> producer = AvroKafkaCreator.createApicurioProducer(
                KafkaAndSchemaRegistryTestResource.getBootstrapServers(),
                KafkaAndSchemaRegistryTestResource.getApicurioSchemaRegistryUrl(),
                "test-avro-apicurio-test");
        testAvroConsumer(producer, APICURIO_PATH, "test-avro-apicurio-consumer");
    }

    private void testAvroProducer(KafkaConsumer<Integer, Pet> consumer, String path) {
        RestAssured.given()
                .header("content-type", "application/json")
                .body("{\"name\":\"neo\", \"color\":\"tricolor\"}")
                .post(path);
        ConsumerRecord<Integer, Pet> records = consumer.poll(Duration.ofMillis(20000)).iterator().next();
        Assertions.assertEquals(records.key(), (Integer) 0);
        Pet pet = records.value();
        Assertions.assertEquals("neo", pet.getName());
        Assertions.assertEquals("tricolor", pet.getColor());
        consumer.close();
    }

    private void testAvroConsumer(KafkaProducer<Integer, Pet> producer, String path, String topic) {
        producer.send(new ProducerRecord<>(topic, 1, createPet()));
        Pet retrieved = RestAssured.when().get(path).as(Pet.class);
        Assertions.assertEquals("neo", retrieved.getName());
        Assertions.assertEquals("white", retrieved.getColor());
        producer.close();
    }

    private Pet createPet() {
        Pet pet = new Pet();
        pet.setName("neo");
        pet.setColor("white");
        return pet;
    }
}
