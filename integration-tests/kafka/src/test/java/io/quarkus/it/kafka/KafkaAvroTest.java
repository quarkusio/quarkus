package io.quarkus.it.kafka;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.quarkus.it.kafka.avro.Pet;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTestResource(SchemaRegistryTestResource.class)
public class KafkaAvroTest {

    public static KafkaConsumer<Integer, Pet> createConsumer() {
        String registry = System.getProperty("schema.url");

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-avro");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registry);

        // Without you get GenericData.Record instead of `Pet`
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        KafkaConsumer<Integer, Pet> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("test-avro-producer"));
        return consumer;
    }

    public static KafkaProducer<Integer, Pet> createProducer() {
        String registry = System.getProperty("schema.url");

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "test-avro");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registry);
        return new KafkaProducer<>(props);
    }

    @Test
    public void testAvroProducer() {
        KafkaConsumer<Integer, Pet> consumer = createConsumer();
        RestAssured.given()
                .header("content-type", "application/json")
                .body("{\"name\":\"neo\", \"color\":\"tricolor\"}")
                .post("/avro");
        ConsumerRecord<Integer, Pet> records = consumer.poll(Duration.ofMillis(20000)).iterator().next();
        Assertions.assertEquals(records.key(), (Integer) 0);
        Pet pet = records.value();
        Assertions.assertEquals("neo", pet.getName());
        Assertions.assertEquals("tricolor", pet.getColor());
        consumer.close();
    }

    @Test
    public void testAvroConsumer() {
        KafkaProducer<Integer, Pet> producer = createProducer();
        Pet pet = new Pet();
        pet.setName("neo");
        pet.setColor("white");
        producer.send(new ProducerRecord<>("test-avro-consumer", 1, pet));
        Pet retrieved = RestAssured.when().get("/avro").as(Pet.class);
        Assertions.assertEquals("neo", retrieved.getName());
        Assertions.assertEquals("white", retrieved.getColor());
        producer.close();
    }

}
