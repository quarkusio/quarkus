package io.quarkus.it.kafka;

import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTest
public class KafkaStreamsTest {

    private static Producer<Integer, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "streams-test-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<Integer, String>(props);
    }

    private static KafkaConsumer<Integer, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "streams-test-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("streams-test-customers-processed"));
        return consumer;
    }

    @Test
    public void testKafkaStreams() throws Exception {
        produceCustomers();

        Consumer<Integer, String> consumer = createConsumer();
        List<ConsumerRecord<Integer, String>> records = poll(consumer, 4);

        ConsumerRecord<Integer, String> record = records.get(0);
        Assertions.assertEquals(101, record.key());
        JsonObject customer = parse(record.value());
        Assertions.assertEquals(101, customer.getInt("id"));
        Assertions.assertEquals("Bob", customer.getString("name"));
        Assertions.assertEquals("B2B", customer.getJsonObject("category").getString("name"));
        Assertions.assertEquals("business-to-business", customer.getJsonObject("category").getString("value"));

        record = records.get(1);
        Assertions.assertEquals(102, record.key());
        customer = parse(record.value());
        Assertions.assertEquals(102, customer.getInt("id"));
        Assertions.assertEquals("Becky", customer.getString("name"));
        Assertions.assertEquals("B2C", customer.getJsonObject("category").getString("name"));
        Assertions.assertEquals("business-to-customer", customer.getJsonObject("category").getString("value"));

        record = records.get(2);
        Assertions.assertEquals(103, record.key());
        customer = parse(record.value());
        Assertions.assertEquals(103, customer.getInt("id"));
        Assertions.assertEquals("Bruce", customer.getString("name"));
        Assertions.assertEquals("B2B", customer.getJsonObject("category").getString("name"));
        Assertions.assertEquals("business-to-business", customer.getJsonObject("category").getString("value"));

        record = records.get(3);
        Assertions.assertEquals(104, record.key());
        customer = parse(record.value());
        Assertions.assertEquals(104, customer.getInt("id"));
        Assertions.assertEquals("Bert", customer.getString("name"));
        Assertions.assertEquals("B2B", customer.getJsonObject("category").getString("name"));
        Assertions.assertEquals("business-to-business", customer.getJsonObject("category").getString("value"));

        assertCategoryCount(1, 3);
        assertCategoryCount(2, 1);

        // explicitly stopping the pipeline *before* the broker is shut down, as it
        // otherwise will time out
        RestAssured.post("/kafkastreams/stop");
    }

    private void produceCustomers() {
        Producer<Integer, String> producer = createProducer();

        producer.send(new ProducerRecord<>("streams-test-categories", 1,
                "{ \"name\" : \"B2B\", \"value\" : \"business-to-business\" }"));
        producer.send(new ProducerRecord<>("streams-test-categories", 2,
                "{ \"name\" : \"B2C\", \"value\" : \"business-to-customer\" }"));

        producer.send(
                new ProducerRecord<>("streams-test-customers", 101, "{ \"id\" : 101, \"name\" : \"Bob\", \"category\" : 1 }"));
        producer.send(new ProducerRecord<>("streams-test-customers", 102,
                "{ \"id\" : 102, \"name\" : \"Becky\", \"category\" : 2 }"));
        producer.send(new ProducerRecord<>("streams-test-customers", 103,
                "{ \"id\" : 103, \"name\" : \"Bruce\", \"category\" : 1 }"));
        producer.send(new ProducerRecord<>("streams-test-customers", 104,
                "{ \"id\" : 104, \"name\" : \"Bert\", \"category\" : 1 }"));
    }

    private void assertCategoryCount(int categoryId, int expectedCount) throws Exception {
        int i = 0;
        Integer actual = null;

        // retrying for some time as the aggregation might not have finished yet
        while (i < 50 && !Integer.valueOf(expectedCount).equals(actual)) {
            actual = getCategoryCount(categoryId);
            Thread.sleep(100);
        }

        Assertions.assertEquals(expectedCount, actual);
    }

    private Integer getCategoryCount(int categoryId) {
        String result = RestAssured.when().get("/kafkastreams/category/" + categoryId).asString();
        if (result != null && !result.trim().isEmpty()) {
            return Integer.valueOf(result);
        }

        return null;
    }

    private List<ConsumerRecord<Integer, String>> poll(Consumer<Integer, String> consumer, int expectedRecordCount) {
        int fetched = 0;
        List<ConsumerRecord<Integer, String>> result = new ArrayList<>();
        while (fetched < expectedRecordCount) {
            ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofMillis(20000));
            records.forEach(result::add);
            fetched = result.size();
        }

        return result;
    }

    private JsonObject parse(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
