package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.split.MultiSplitter;

@ApplicationScoped
public class FruitSplitProducer {

    @Incoming("fruits-out")
    @Outgoing("fruit-berry")
    @Outgoing("fruit-citrus")
    @Outgoing("fruit-pome")
    @Outgoing("fruit-stone")
    @Outgoing("fruit-tropical")
    MultiSplitter<Fruit, Fruit.Fruits> produce(Multi<Fruit> fruits) {
        return fruits.split(Fruit.Fruits.class, s -> s.type);
    }
}
