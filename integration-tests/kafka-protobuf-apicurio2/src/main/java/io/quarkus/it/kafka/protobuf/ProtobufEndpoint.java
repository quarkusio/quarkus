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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.json.JsonObject;

/**
 * Endpoint to test the Protobuf support with both Apicurio and Confluent registries
 */
@RegisterForReflection(targets = { com.example.tutorial.PetOuterClass.Pet.class,
        com.example.tutorial.PetOuterClass.Pet.Builder.class })
@Path("/protobuf")
public class ProtobufEndpoint {

    @Inject
    ProtobufKafkaCreator creator;

    // --- Apicurio endpoints ---

    @GET
    @Path("/apicurio")
    public JsonObject getApicurio() {
        return get(creator.createApicurioConsumer("test-protobuf-apicurio-consumer", "test-protobuf-apicurio"));
    }

    @POST
    @Path("/apicurio")
    public void sendApicurio(Pet pet) {
        KafkaProducer<Integer, com.example.tutorial.PetOuterClass.Pet> p = creator
                .createApicurioProducer("test-protobuf-apicurio");
        send(p, pet, "test-protobuf-apicurio");
    }

    // --- Confluent endpoints ---

    @GET
    @Path("/confluent")
    public JsonObject getConfluent() {
        return get(creator.createConfluentConsumer("test-protobuf-confluent-consumer", "test-protobuf-confluent"));
    }

    @POST
    @Path("/confluent")
    public void sendConfluent(Pet pet) {
        KafkaProducer<Integer, com.example.tutorial.PetOuterClass.Pet> p = creator
                .createConfluentProducer("test-protobuf-confluent");
        send(p, pet, "test-protobuf-confluent");
    }

    // --- Common helpers ---

    private JsonObject get(KafkaConsumer<Integer, Message> consumer) {
        final ConsumerRecords<Integer, Message> records = consumer.poll(Duration.ofMillis(60000));
        if (records.isEmpty()) {
            return null;
        }
        ConsumerRecord<Integer, Message> consumerRecord = records.iterator().next();
        Message msg = consumerRecord.value();
        // Extract fields from the protobuf message using the descriptor API.
        // The deserializer may return a DynamicMessage instead of the specific
        // generated class due to Quarkus classloader boundaries, so we use the
        // generic Message interface and extract fields by name.
        JsonObject result = new JsonObject();
        for (Descriptors.FieldDescriptor field : msg.getDescriptorForType().getFields()) {
            result.put(field.getName(), String.valueOf(msg.getField(field)));
        }
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
