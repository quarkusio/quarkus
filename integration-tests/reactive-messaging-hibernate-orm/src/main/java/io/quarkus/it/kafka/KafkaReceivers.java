package io.quarkus.it.kafka;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.commit.CheckpointMetadata;

@ApplicationScoped
public class KafkaReceivers {

    private final List<Person> people = new CopyOnWriteArrayList<>();

    @Channel("fruits-persisted")
    MutinyEmitter<Fruit> emitter;

    @Incoming("fruits-in")
    @Transactional
    public CompletionStage<Void> persist(Message<Fruit> fruit) {
        fruit.getPayload().persist();
        return emitter.sendMessage(fruit).subscribeAsCompletionStage();
    }

    @Incoming("people-in")
    public CompletionStage<Void> consume(Message<Person> msg) {
        CheckpointMetadata<PeopleState> store = CheckpointMetadata.fromMessage(msg);
        Person person = msg.getPayload();
        store.transform(new PeopleState(), c -> {
            if (c.names == null) {
                c.names = person.getName();
            } else {
                c.names = c.names + ";" + person.getName();
            }
            return c;
        });
        people.add(person);
        return msg.ack();
    }

    public List<Fruit> getFruits() {
        return Fruit.listAll();
    }

    public List<Person> getPeople() {
        return people;
    }

}
