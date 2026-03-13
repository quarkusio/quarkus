package io.quarkus.it.kafka.protobuf;

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

import io.apicurio.registry.serde.config.SerdeConfig;
import io.apicurio.registry.serde.protobuf.ProtobufKafkaDeserializer;
import io.apicurio.registry.serde.protobuf.ProtobufKafkaSerializer;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;

/**
 * Create Protobuf Kafka Consumers and Producers for Apicurio and Confluent registries
 */
@ApplicationScoped
public class ProtobufKafkaCreator {

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String bootstrap;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.apicurio.registry.url")
    String apicurioRegistryUrl;

    @ConfigProperty(name = "mp.messaging.connector.smallrye-kafka.schema.registry.url")
    String confluentRegistryUrl;

    public ProtobufKafkaCreator() {
    }

    // --- Apicurio ---

    public KafkaConsumer<Integer, com.google.protobuf.Message> createApicurioConsumer(
            String groupIdConfig, String subscriptionName) {
        Properties props = getApicurioConsumerProperties(bootstrap, apicurioRegistryUrl, groupIdConfig);
        if (!props.containsKey(ConsumerConfig.CLIENT_ID_CONFIG)) {
            props.put(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        }
        KafkaConsumer<Integer, com.google.protobuf.Message> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(subscriptionName));
        return consumer;
    }

    public KafkaProducer<Integer, com.example.tutorial.PetOuterClass.Pet> createApicurioProducer(
            String clientId) {
        Properties props = getApicurioProducerProperties(bootstrap, apicurioRegistryUrl, clientId);
        return new KafkaProducer<>(props);
    }

    // --- Confluent ---

    public KafkaConsumer<Integer, com.google.protobuf.Message> createConfluentConsumer(
            String groupIdConfig, String subscriptionName) {
        Properties props = getConfluentConsumerProperties(bootstrap, confluentRegistryUrl, groupIdConfig);
        if (!props.containsKey(ConsumerConfig.CLIENT_ID_CONFIG)) {
            props.put(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString());
        }
        KafkaConsumer<Integer, com.google.protobuf.Message> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(subscriptionName));
        return consumer;
    }

    public KafkaProducer<Integer, com.example.tutorial.PetOuterClass.Pet> createConfluentProducer(
            String clientId) {
        Properties props = getConfluentProducerProperties(bootstrap, confluentRegistryUrl, clientId);
        return new KafkaProducer<>(props);
    }

    // --- Apicurio properties ---

    private Properties getApicurioConsumerProperties(String bootstrap, String apicurio, String groupIdConfig) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupIdConfig);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ProtobufKafkaDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(SerdeConfig.REGISTRY_URL, apicurio);
        return props;
    }

    private Properties getApicurioProducerProperties(String bootstrap, String apicurio, String clientId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ProtobufKafkaSerializer.class.getName());
        props.put(SerdeConfig.REGISTRY_URL, apicurio);
        props.put(SerdeConfig.AUTO_REGISTER_ARTIFACT, true);
        return props;
    }

    // --- Confluent properties ---

    private Properties getConfluentConsumerProperties(String bootstrap, String confluent, String groupIdConfig) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupIdConfig);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, confluent);
        return props;
    }

    private Properties getConfluentProducerProperties(String bootstrap, String confluent, String clientId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class.getName());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, confluent);
        props.put("auto.register.schemas", true);
        return props;
    }
}
