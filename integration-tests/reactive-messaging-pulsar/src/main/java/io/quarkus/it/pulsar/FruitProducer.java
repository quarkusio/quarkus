package io.quarkus.it.pulsar;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import examples.HelloRequest;
import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class FruitProducer {

    @Outgoing("fruits-out")
    public Multi<HelloRequest> generateFruits() {
        return Multi.createFrom().items(
                HelloRequest.newBuilder().setName("apple").build(),
                HelloRequest.newBuilder().setName("banana").build(),
                HelloRequest.newBuilder().setName("peach").build(),
                HelloRequest.newBuilder().setName("orange").build());
    }
}
