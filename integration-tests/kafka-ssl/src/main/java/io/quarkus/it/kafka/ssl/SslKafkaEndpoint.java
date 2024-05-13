package io.quarkus.it.kafka.ssl;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.Config;

/**
 * Endpoint to check the SSL connection.
 */
@Path("/ssl")
public class SslKafkaEndpoint {

    @Inject
    Config config;

    @GET
    public String get(@QueryParam("format") CertificateFormat format) {
        Consumer<Integer, String> consumer = createConsumer(format);
        final ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        consumer.close();
        return records.iterator().next().value();
    }

    public KafkaConsumer<Integer, String> createConsumer(CertificateFormat format) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getValue("kafka.bootstrap.servers", String.class));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        String truststore = switch (format) {
            case PKCS12 -> "kafka-truststore.p12";
            case JKS -> "kafka-truststore.jks";
            case PEM -> "kafka-ca.crt";
        };

        File tsFile = new File(config.getValue("ssl-dir", String.class), truststore);
        props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        props.setProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, tsFile.getPath());
        if (format != CertificateFormat.PEM) {
            props.setProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L");
        }
        props.setProperty(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, format.name());
        props.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

        KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("test-ssl-consumer"));
        return consumer;
    }
}
