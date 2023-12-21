package io.quarkus.it.kafka;

import java.time.Duration;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.example.tutorial.PetOuterClass.Pet;

import io.quarkus.it.kafka.protobuf.ProtobufKafkaCreator;
import io.restassured.RestAssured;

public abstract class KafkaProtobufTestBase {

    static final String APICURIO_PATH = "/protobuf/apicurio";

    abstract ProtobufKafkaCreator creator();

    @Test
    public void testUrls() {
        Assertions.assertTrue(creator().getApicurioRegistryUrl().endsWith("/apis/registry/v2"));
    }

    @Test
    public void testApicurioProtobufProducer() {
        KafkaConsumer<Integer, Pet> consumer = creator().createApicurioConsumer(
                "test-protobuf-apicurio",
                "test-protobuf-apicurio-producer");
        testProtobufProducer(consumer, APICURIO_PATH);
    }

    @Test
    public void testApicurioProtobufConsumer() {
        KafkaProducer<Integer, Pet> producer = creator().createApicurioProducer("test-protobuf-apicurio-test");
        testProtobufConsumer(producer, APICURIO_PATH, "test-protobuf-apicurio-consumer");
    }

    protected void testProtobufProducer(KafkaConsumer<Integer, Pet> consumer, String path) {
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

    protected void testProtobufConsumer(KafkaProducer<Integer, Pet> producer, String path, String topic) {
        producer.send(new ProducerRecord<>(topic, 1, createPet()));
        producer.close();
    }

    private Pet createPet() {
        return Pet.newBuilder()
                .setName("neo")
                .setColor("white").build();
    }
}
