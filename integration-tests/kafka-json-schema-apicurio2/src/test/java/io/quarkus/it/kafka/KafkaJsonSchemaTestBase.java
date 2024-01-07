package io.quarkus.it.kafka;

import java.time.Duration;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.it.kafka.jsonschema.JsonSchemaKafkaCreator;
import io.quarkus.it.kafka.jsonschema.Pet;
import io.restassured.RestAssured;

public abstract class KafkaJsonSchemaTestBase {

    static final String APICURIO_PATH = "/json-schema/apicurio";

    static final String CONFLUENT_PATH = "/json-schema/confluent";

    abstract JsonSchemaKafkaCreator creator();

    @Test
    public void testUrls() {
        Assertions.assertTrue(creator().getApicurioRegistryUrl().endsWith("/apis/registry/v2"));
    }

    @Test
    public void testApicurioJsonSchemaProducer() {
        String subscriptionName = "test-json-schema-apicurio-producer";

        KafkaConsumer<Integer, Pet> consumer = creator().createApicurioConsumer(
                "test-json-schema-apicurio",
                subscriptionName);
        testJsonSchemaProducer(consumer, APICURIO_PATH);
    }

    @Test
    public void testApicurioJsonSchemaConsumer() {
        String topic = "test-json-schema-apicurio-consumer";
        KafkaProducer<Integer, Pet> producer = creator().createApicurioProducer("test-json-schema-apicurio-test");
        testJsonSchemaConsumer(producer, APICURIO_PATH, topic);
    }

    @Test
    public void testConfluentJsonSchemaProducer() {
        KafkaConsumer<Integer, Pet> consumer = creator().createConfluentConsumer(
                "test-json-schema-confluent",
                "test-json-schema-confluent-producer");
        testJsonSchemaProducer(consumer, CONFLUENT_PATH);
    }

    @Test
    public void testConfluentJsonSchemaConsumer() {
        KafkaProducer<Integer, Pet> producer = creator().createConfluentProducer("test-json-schema-confluent-test");
        testJsonSchemaConsumer(producer, CONFLUENT_PATH, "test-json-schema-confluent-consumer");
    }

    protected void testJsonSchemaProducer(KafkaConsumer<Integer, Pet> consumer, String path) {
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

    protected void testJsonSchemaConsumer(KafkaProducer<Integer, Pet> producer, String path, String topic) {
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
