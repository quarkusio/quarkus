package org.jboss.shamrock.example.test;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hamcrest.Matchers;
import org.jboss.shamrock.test.common.ShamrockTestResource;
import org.jboss.shamrock.test.junit.ShamrockTest;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

@ShamrockTestResource(KafkaTestResource.class)
@ShamrockTest
public class KafkaConsumerTest {


    public static Producer<Integer, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "test-consumer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<Integer, String>(props);
    }

    @Test
    public void test() throws Exception {
        Producer<Integer, String> consumer = createProducer();
        consumer.send(new ProducerRecord<>("test-consumer", 1, "hi world"));
        RestAssured.when().get("/kafka").then().body(Matchers.is("hi world"));
    }


}
