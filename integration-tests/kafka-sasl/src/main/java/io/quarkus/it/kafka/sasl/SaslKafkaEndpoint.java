package io.quarkus.it.kafka.sasl;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.Config;

/**
 * Endpoint to check the SSL/SASL connection.
 */
@Path("/sasl")
public class SaslKafkaEndpoint {

    private Consumer<Integer, String> consumer;

    @Inject
    Config config;

    @PostConstruct
    public void create() {
        consumer = createConsumer();
    }

    @GET
    public String get() {
        final ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        return records.iterator().next().value();
    }

    private static void addJAAS(Properties props) {
        props.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        props.setProperty(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.setProperty(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required "
                        + "username=\"client\" "
                        + "password=\"client-secret\";");
    }

    public KafkaConsumer<Integer, String> createConsumer() {
        String bs = config.getValue("kafka.bootstrap.servers", String.class);
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bs);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        addJAAS(props);
        KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("test-sasl-consumer"));
        return consumer;
    }
}
