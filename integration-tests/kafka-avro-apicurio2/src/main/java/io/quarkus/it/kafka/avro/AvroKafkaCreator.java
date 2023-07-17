package io.quarkus.it.kafka.avro;

import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.apicurio.registry.serde.SerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroKafkaSerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

/**
 * Create Avro Kafka Consumers and Producers
 */
@ApplicationScoped
public class AvroKafkaCreator {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrap;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.apicurio.registry.url")
    String apicurioRegistryUrl;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.schema.registry.url")
    String confluentRegistryUrl;

    public AvroKafkaCreator() {
    }

    public AvroKafkaCreator(String bootstrap, String apicurioRegistryUrl, String confluentRegistryUrl) {
        this.bootstrap = bootstrap;
        this.apicurioRegistryUrl = apicurioRegistryUrl;
        this.confluentRegistryUrl = confluentRegistryUrl;
    }

    public String getApicurioRegistryUrl() {
        return apicurioRegistryUrl;
    }

    public String getConfluentRegistryUrl() {
        return confluentRegistryUrl;
    }

    public KafkaConsumer<Integer, Pet> createConfluentConsumer(String groupdIdConfig, String subscribtionName) {
        return createConfluentConsumer(bootstrap, getConfluentRegistryUrl(), groupdIdConfig, subscribtionName);
    }

    public KafkaProducer<Integer, Pet> createConfluentProducer(String clientId) {
        return createConfluentProducer(bootstrap, getConfluentRegistryUrl(), clientId);
    }

    public KafkaConsumer<Integer, Pet> createApicurioConsumer(String groupdIdConfig, String subscribtionName) {
        return createApicurioConsumer(bootstrap, getApicurioRegistryUrl(), groupdIdConfig, subscribtionName);
    }

    public KafkaProducer<Integer, Pet> createApicurioProducer(String clientId) {
        return createApicurioProducer(bootstrap, getApicurioRegistryUrl(), clientId);
    }

    public static KafkaConsumer<Integer, Pet> createConfluentConsumer(String bootstrap, String confluent,
            String groupdIdConfig, String subscribtionName) {
        Properties p = getConfluentConsumerProperties(bootstrap, confluent, groupdIdConfig);
        return createConsumer(p, subscribtionName);
    }

    public static KafkaConsumer<Integer, Pet> createApicurioConsumer(String bootstrap, String apicurio,
            String groupdIdConfig, String subscribtionName) {
        Properties p = getApicurioConsumerProperties(bootstrap, apicurio, groupdIdConfig);
        return createConsumer(p, subscribtionName);
    }

    public static KafkaProducer<Integer, Pet> createConfluentProducer(String bootstrap, String confluent,
            String clientId) {
        Properties p = getConfluentProducerProperties(bootstrap, confluent, clientId);
        return createProducer(p);
    }

    public static KafkaProducer<Integer, Pet> createApicurioProducer(String bootstrap, String apicurio,
            String clientId) {
        Properties p = getApicurioProducerProperties(bootstrap, apicurio, clientId);
        return createProducer(p);
    }

    private static KafkaConsumer<Integer, Pet> createConsumer(Properties props, String subscribtionName) {
        if (!props.containsKey(ConsumerConfig.CLIENT_ID_CONFIG)) {
            props.put(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        }
        KafkaConsumer<Integer, Pet> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(subscribtionName));
        return consumer;
    }

    private static KafkaProducer<Integer, Pet> createProducer(Properties props) {
        if (!props.containsKey(ProducerConfig.CLIENT_ID_CONFIG)) {
            props.put(ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        }
        return new KafkaProducer<>(props);
    }

    private static Properties getConfluentConsumerProperties(String bootstrap, String confluent,
            String groupdIdConfig) {
        Properties props = getGenericConsumerProperties(bootstrap, groupdIdConfig);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, confluent);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        return props;
    }

    public static Properties getApicurioConsumerProperties(String bootstrap, String apicurio, String groupdIdConfig) {
        Properties props = getGenericConsumerProperties(bootstrap, groupdIdConfig);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class.getName());
        props.put(SerdeConfig.REGISTRY_URL, apicurio);
        props.put(AvroKafkaSerdeConfig.USE_SPECIFIC_AVRO_READER, true);
        return props;
    }

    private static Properties getGenericConsumerProperties(String bootstrap, String groupdIdConfig) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupdIdConfig);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        return props;
    }

    private static Properties getConfluentProducerProperties(String bootstrap, String confluent, String clientId) {
        Properties props = getGenericProducerProperties(bootstrap, clientId);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, confluent);
        return props;
    }

    private static Properties getApicurioProducerProperties(String bootstrap, String apicurio, String clientId) {
        Properties props = getGenericProducerProperties(bootstrap, clientId);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class.getName());
        props.put(SerdeConfig.REGISTRY_URL, apicurio);
        props.put(SerdeConfig.AUTO_REGISTER_ARTIFACT, true);
        return props;
    }

    private static Properties getGenericProducerProperties(String bootstrap, String clientId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        return props;
    }
}
