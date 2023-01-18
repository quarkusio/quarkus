package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class FruitProducer {
    @Outgoing("fruits-out")
    public Multi<Fruit> generateFruits() {
        return Multi.createFrom().items(
                new Fruit("apple"),
                new Fruit("banana"),
                new Fruit("peach"),
                new Fruit("orange"));
    }
}
