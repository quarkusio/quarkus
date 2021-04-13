package io.quarkus.it.kafka;

import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(KafkaSASLTestResource.class)
public class SaslKafkaConsumerTest {

    public Producer<Integer, String> createProducer() {
        Properties props = new Properties();
        String bs = System.getProperty("bootstrap.servers");
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "sasl-test-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        props.setProperty(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.setProperty(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required "
                        + "username=\"client\" "
                        + "password=\"client-secret\";");

        return new KafkaProducer<>(props);
    }

    @Test
    public void testReception() {
        Producer<Integer, String> consumer = createProducer();
        consumer.send(new ProducerRecord<>("test-sasl-consumer", 1, "hi world"));
        String string = RestAssured.when().get("/sasl").andReturn().asString();
        Assertions.assertEquals("hi world", string);
    }

}
