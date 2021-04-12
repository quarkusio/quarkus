package io.quarkus.it.kafka;

import java.io.File;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(KafkaSSLTestResource.class)
public class SslKafkaConsumerTest {

    public static Producer<Integer, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("bootstrap.servers"));
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "test-ssl-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        File tsFile = new File("src/test/resources/kafka-truststore.p12");
        props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, tsFile.getPath());
        props.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L");
        props.setProperty(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
        props.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

        return new KafkaProducer<>(props);
    }

    @Test
    public void testReception() {
        Producer<Integer, String> consumer = createProducer();
        consumer.send(new ProducerRecord<>("test-ssl-consumer", 1, "hi world"));
        String string = RestAssured.when().get("/ssl").andReturn().asString();
        Assertions.assertEquals("hi world", string);
    }

}
