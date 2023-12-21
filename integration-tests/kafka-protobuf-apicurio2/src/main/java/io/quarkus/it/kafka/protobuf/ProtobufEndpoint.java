package io.quarkus.it.kafka.protobuf;

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
 * Endpoint to test the Protobuf support
 */
@Path("/protobuf")
public class ProtobufEndpoint {

    @Inject
    ProtobufKafkaCreator creator;

    @GET
    @Path("/apicurio")
    public JsonObject getApicurio() {
        return get(creator.createApicurioConsumer("test-protobuf-apicurio-consumer", "test-protobuf-apicurio-consumer"));
    }

    @POST
    @Path("/apicurio")
    public void sendApicurio(io.quarkus.it.kafka.protobuf.Pet pet) {
        KafkaProducer<Integer, com.example.tutorial.PetOuterClass.Pet> p = creator
                .createApicurioProducer("test-protobuf-apicurio");
        send(p, pet, "test-protobuf-apicurio-producer");
    }

    private JsonObject get(KafkaConsumer<Integer, com.example.tutorial.PetOuterClass.Pet> consumer) {
        final ConsumerRecords<Integer, com.example.tutorial.PetOuterClass.Pet> records = consumer
                .poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        ConsumerRecord<Integer, com.example.tutorial.PetOuterClass.Pet> consumerRecord = records.iterator().next();
        com.example.tutorial.PetOuterClass.Pet p = consumerRecord.value();
        // We cannot serialize the returned Pet directly, it contains non-serializable object such as the schema.
        JsonObject result = new JsonObject();
        result.put("name", p.getName());
        result.put("color", p.getColor());
        return result;
    }

    private void send(KafkaProducer<Integer, com.example.tutorial.PetOuterClass.Pet> producer, Pet pet, String topic) {
        com.example.tutorial.PetOuterClass.Pet protoPet = com.example.tutorial.PetOuterClass.Pet.newBuilder()
                .setColor(pet.getColor())
                .setName(pet.getName())
                .build();

        producer.send(new ProducerRecord<>(topic, 0, protoPet));
        producer.flush();
    }
}
