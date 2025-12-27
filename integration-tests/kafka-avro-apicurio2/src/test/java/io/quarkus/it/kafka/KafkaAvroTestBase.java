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
import io.restassured.RestAssured;

public abstract class KafkaAvroTestBase {

    static final String CONFLUENT_PATH = "/avro/confluent";
    static final String APICURIO_PATH = "/avro/apicurio";

    abstract AvroKafkaCreator creator();

    @Test
    public void testUrls() {
        Assertions.assertTrue(creator().getApicurioRegistryUrl().endsWith("/apis/registry/v3"));
        Assertions.assertTrue(creator().getConfluentRegistryUrl().endsWith("/apis/ccompat/v7"));
    }

    @Test
    public void testConfluentAvroProducer() {
        KafkaConsumer<Integer, Pet> consumer = creator().createConfluentConsumer(
                "test-avro-confluent",
                "test-avro-confluent-producer");
        testAvroProducer(consumer, CONFLUENT_PATH);
    }

    @Test
    public void testConfluentAvroConsumer() {
        KafkaProducer<Integer, Pet> producer = creator().createConfluentProducer("test-avro-confluent-test");
        testAvroConsumer(producer, CONFLUENT_PATH, "test-avro-confluent-consumer");
    }

    @Test
    public void testApicurioAvroProducer() {
        KafkaConsumer<Integer, Pet> consumer = creator().createApicurioConsumer(
                "test-avro-apicurio",
                "test-avro-apicurio-producer");
        testAvroProducer(consumer, APICURIO_PATH);
    }

    @Test
    public void testApicurioAvroConsumer() {
        KafkaProducer<Integer, Pet> producer = creator().createApicurioProducer("test-avro-apicurio-test");
        testAvroConsumer(producer, APICURIO_PATH, "test-avro-apicurio-consumer");
    }

    protected void testAvroProducer(KafkaConsumer<Integer, Pet> consumer, String path) {
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

    protected void testAvroConsumer(KafkaProducer<Integer, Pet> producer, String path, String topic) {
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
