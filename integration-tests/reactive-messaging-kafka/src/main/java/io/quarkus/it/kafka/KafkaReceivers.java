package io.quarkus.it.kafka;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.kafka.Record;
import io.smallrye.reactive.messaging.kafka.commit.CheckpointMetadata;

@ApplicationScoped
public class KafkaReceivers {

    private final List<Person> people = new CopyOnWriteArrayList<>();
    private final List<Fruit> fruits = new CopyOnWriteArrayList<>();
    private final List<Record<Pet, Person>> pets = new CopyOnWriteArrayList<>();

    static class PeopleState {
        public String names;
    }

    @Incoming("people-in")
    public CompletionStage<Void> consume(Message<Person> msg) {
        CheckpointMetadata<PeopleState> store = CheckpointMetadata.fromMessage(msg);
        Person person = msg.getPayload();
        store.transform(new PeopleState(), c -> {
            if (c.names == null || c.names.length() == 0) {
                c.names = person.getName();
            } else {
                c.names = c.names + ";" + person.getName();
            }
            return c;
        });
        people.add(person);
        return msg.ack();
    }

    @Incoming("fruits-in")
    public void consume(Fruit fruit) {
        fruits.add(fruit);
    }

    @Incoming("pets-in")
    public void consume(Record<Pet, Person> pet) {
        pets.add(pet);
    }

    public List<Person> getPeople() {
        return people;
    }

    public List<Fruit> getFruits() {
        return fruits;
    }

    public List<Record<Pet, Person>> getPets() {
        return pets;
    }
}
