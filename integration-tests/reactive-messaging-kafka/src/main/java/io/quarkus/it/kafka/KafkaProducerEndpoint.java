package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.Record;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

@ApplicationScoped
@Path("/kafka")
public class KafkaProducerEndpoint {

    @Inject
    @Channel("pets-out")
    MutinyEmitter<Record<Pet, Person>> petsEmitter;

    @Inject
    @Channel("data-with-metadata-out")
    MutinyEmitter<String> dataWithMetadataEmitter;

    @Inject
    @Channel("data-for-keyed-out")
    MutinyEmitter<String> dataForKeyedEmitter;

    @POST
    @Path("/produce-pets")
    public void producePets() {
        petsEmitter.sendAndAwait(Record.of(new Pet("dog"), new Person("john")));
        petsEmitter.sendAndAwait(Record.of(new Pet("dog"), new Person("james")));
        petsEmitter.sendAndAwait(Record.of(new Pet("rabbit"), new Person("clement")));
    }

    @POST
    @Path("/produce-data-with-metadata")
    public void produceDataWithMetadata() {
        dataWithMetadataEmitter.sendMessageAndAwait(
                Message.of("a").addMetadata(OutgoingKafkaRecordMetadata.builder().withKey("a").build()));
        dataWithMetadataEmitter.sendMessageAndAwait(
                Message.of("b").addMetadata(OutgoingKafkaRecordMetadata.builder().withKey("b").build()));
        dataWithMetadataEmitter.sendMessageAndAwait(
                Message.of("c").addMetadata(OutgoingKafkaRecordMetadata.builder().withKey("a").build()));
    }

    @POST
    @Path("/produce-data-for-keyed")
    public void produceDataForKeyed() {
        dataForKeyedEmitter.sendMessageAndAwait(
                Message.of("a").addMetadata(OutgoingKafkaRecordMetadata.builder().withKey("a").build()));
        dataForKeyedEmitter.sendMessageAndAwait(
                Message.of("b").addMetadata(OutgoingKafkaRecordMetadata.builder().withKey("b").build()));
        dataForKeyedEmitter.sendMessageAndAwait(
                Message.of("c").addMetadata(OutgoingKafkaRecordMetadata.builder().withKey("a").build()));
    }
}
