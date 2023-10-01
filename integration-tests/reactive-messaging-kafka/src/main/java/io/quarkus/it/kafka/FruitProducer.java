package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class FruitProducer {
    @Outgoing("fruits-out")
    public Multi<Fruit> generateFruits() {
        return Multi.createFrom().items(
                Fruit.Fruits.BERRY.create("strawberry"),
                Fruit.Fruits.POME.create("apple"),
                Fruit.Fruits.TROPICAL.create("banana"),
                Fruit.Fruits.STONE.create("peach"),
                Fruit.Fruits.CITRUS.create("orange"));
    }
}
