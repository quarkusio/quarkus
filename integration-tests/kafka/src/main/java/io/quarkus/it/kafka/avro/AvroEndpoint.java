package io.quarkus.it.kafka.avro;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.vertx.core.json.JsonObject;

/**
 * Endpoint to test the Avro support
 */
@Path("/avro")
public class AvroEndpoint {

    private KafkaConsumer<Integer, Pet> consumer;
    private KafkaProducer<Integer, Pet> producer;

    @PostConstruct
    public void init() {
        String registry = System.getProperty("schema.url");
        producer = createProducer(registry);
        consumer = createConsumer(registry);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject get() {
        final ConsumerRecords<Integer, Pet> records = consumer.poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        Pet p = records.iterator().next().value();
        // We cannot serialize the returned Pet directly, it contains non-serializable object such as the schema.
        JsonObject result = new JsonObject();
        result.put("name", p.getName());
        result.put("color", p.getColor());
        return result;
    }

    @POST
    public void send(Pet pet) {
        producer.send(new ProducerRecord<>("test-avro-producer", 0, pet));
        producer.flush();
    }

    public static KafkaConsumer<Integer, Pet> createConsumer(String registry) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-avro-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registry);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        KafkaConsumer<Integer, Pet> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("test-avro-consumer"));
        return consumer;
    }

    public static KafkaProducer<Integer, Pet> createProducer(String registry) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "test-avro");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, registry);
        return new KafkaProducer<>(props);
    }
}
