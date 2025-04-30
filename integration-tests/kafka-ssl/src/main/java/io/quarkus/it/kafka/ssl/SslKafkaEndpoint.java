package io.quarkus.it.kafka.ssl;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.smallrye.common.annotation.Identifier;

/**
 * Endpoint to check the SSL connection.
 */
@Path("/ssl")
public class SslKafkaEndpoint {

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> kafkaConfig;

    @Inject
    KafkaAdminClient adminClient;

    @GET
    public String get(@QueryParam("format") CertificateFormat format) throws ExecutionException, InterruptedException {
        // prevent admin client to be removed
        adminClient.getTopics();
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
        props.putAll(kafkaConfig);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("test-ssl-consumer"));
        return consumer;
    }
}
