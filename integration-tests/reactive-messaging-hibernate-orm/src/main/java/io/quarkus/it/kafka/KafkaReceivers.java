package io.quarkus.it.kafka;

import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.MutinyEmitter;

@ApplicationScoped
public class KafkaReceivers {

    @Channel("fruits-persisted")
    MutinyEmitter<Fruit> emitter;

    @Incoming("fruits-in")
    @Transactional
    public CompletionStage<Void> persist(Message<Fruit> fruit) {
        fruit.getPayload().persist();
        return emitter.sendMessage(fruit).subscribeAsCompletionStage();
    }

    public List<Fruit> getFruits() {
        return Fruit.listAll();
    }

}
