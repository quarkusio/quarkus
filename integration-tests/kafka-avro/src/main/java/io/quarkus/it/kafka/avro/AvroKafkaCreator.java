package io.quarkus.it.kafka.avro;

import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;

import io.apicurio.registry.utils.serde.AbstractKafkaSerDe;
import io.apicurio.registry.utils.serde.AbstractKafkaSerializer;
import io.apicurio.registry.utils.serde.AvroKafkaDeserializer;
import io.apicurio.registry.utils.serde.AvroKafkaSerializer;
import io.apicurio.registry.utils.serde.avro.AvroDatumProvider;
import io.apicurio.registry.utils.serde.avro.ReflectAvroDatumProvider;
import io.apicurio.registry.utils.serde.strategy.GetOrCreateIdStrategy;
import io.apicurio.registry.utils.serde.strategy.SimpleTopicIdStrategy;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

/**
 * Create Avro Kafka Consumers and Producers
 */
public class AvroKafkaCreator {

    public static KafkaConsumer<Integer, Pet> createConfluentConsumer(String groupdIdConfig, String subscribtionName) {
        Properties p = getConfluentConsumerProperties(groupdIdConfig);
        return createConsumer(p, subscribtionName);
    }

    public static KafkaConsumer<Integer, Pet> createApicurioConsumer(String groupdIdConfig, String subscribtionName) {
        Properties p = getApicurioConsumerProperties(groupdIdConfig);
        return createConsumer(p, subscribtionName);
    }

    public static KafkaProducer<Integer, Pet> createConfluentProducer(String clientId) {
        Properties p = getConfluentProducerProperties(clientId);
        return createProducer(p);
    }

    public static KafkaProducer<Integer, Pet> createApicurioProducer(String clientId) {
        Properties p = getApicurioProducerProperties(clientId);
        return createProducer(p);
    }

    private static KafkaConsumer<Integer, Pet> createConsumer(Properties props, String subscribtionName) {
        KafkaConsumer<Integer, Pet> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(subscribtionName));
        return consumer;
    }

    private static KafkaProducer<Integer, Pet> createProducer(Properties props) {
        return new KafkaProducer<>(props);
    }

    private static Properties getConfluentConsumerProperties(String groupdIdConfig) {
        Properties props = getGenericConsumerProperties(groupdIdConfig);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, System.getProperty("schema.url.confluent"));
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        return props;
    }

    public static Properties getApicurioConsumerProperties(String groupdIdConfig) {
        Properties props = getGenericConsumerProperties(groupdIdConfig);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class.getName());
        props.put(AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM, System.getProperty("schema.url.apicurio"));
        props.put(AvroDatumProvider.REGISTRY_AVRO_DATUM_PROVIDER_CONFIG_PARAM, ReflectAvroDatumProvider.class.getName());
        return props;
    }

    private static Properties getGenericConsumerProperties(String groupdIdConfig) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupdIdConfig);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        return props;
    }

    private static Properties getConfluentProducerProperties(String clientId) {
        Properties props = getGenericProducerProperties(clientId);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, System.getProperty("schema.url.confluent"));
        return props;
    }

    private static Properties getApicurioProducerProperties(String clientId) {
        Properties props = getGenericProducerProperties(clientId);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class.getName());
        props.put(AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM, System.getProperty("schema.url.apicurio"));
        props.put(AbstractKafkaSerializer.REGISTRY_ARTIFACT_ID_STRATEGY_CONFIG_PARAM, SimpleTopicIdStrategy.class.getName());
        props.put(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM, GetOrCreateIdStrategy.class.getName());
        props.put(AvroDatumProvider.REGISTRY_AVRO_DATUM_PROVIDER_CONFIG_PARAM, ReflectAvroDatumProvider.class.getName());
        return props;
    }

    private static Properties getGenericProducerProperties(String clientId) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        return props;
    }
}
