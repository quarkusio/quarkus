package io.quarkus.it.kafka.avro;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import io.vertx.core.json.JsonObject;

/**
 * Endpoint to test the Avro support
 */
@Path("/avro")
public class AvroEndpoint {

    @Inject
    AvroKafkaCreator creator;

    @GET
    @Path("/confluent")
    public JsonObject getConfluent() {
        return get(creator.createConfluentConsumer("test-avro-confluent-consumer", "test-avro-confluent-consumer"));
    }

    @POST
    @Path("/confluent")
    public void sendConfluent(Pet pet) {
        KafkaProducer<Integer, Pet> p = creator.createConfluentProducer("test-avro-confluent");
        send(p, pet, "test-avro-confluent-producer");
    }

    @GET
    @Path("/apicurio")
    public JsonObject getApicurio() {
        return get(creator.createApicurioConsumer("test-avro-apicurio-consumer", "test-avro-apicurio-consumer"));
    }

    @POST
    @Path("/apicurio")
    public void sendApicurio(Pet pet) {
        KafkaProducer<Integer, Pet> p = creator.createApicurioProducer("test-avro-apicurio");
        send(p, pet, "test-avro-apicurio-producer");
    }

    private JsonObject get(KafkaConsumer<Integer, Pet> consumer) {
        final ConsumerRecords<Integer, Pet> records = consumer.poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        ConsumerRecord<Integer, Pet> consumerRecord = records.iterator().next();
        Pet p = consumerRecord.value();
        // We cannot serialize the returned Pet directly, it contains non-serializable object such as the schema.
        JsonObject result = new JsonObject();
        result.put("name", p.getName());
        result.put("color", p.getColor());
        return result;
    }

    private void send(KafkaProducer<Integer, Pet> producer, Pet pet, String topic) {
        producer.send(new ProducerRecord<>(topic, 0, pet));
        producer.flush();
    }
}
