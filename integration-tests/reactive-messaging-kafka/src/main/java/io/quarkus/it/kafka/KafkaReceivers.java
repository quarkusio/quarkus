package io.quarkus.it.kafka;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.smallrye.reactive.messaging.kafka.Record;

@ApplicationScoped
public class KafkaReceivers {

    private final List<Person> people = new CopyOnWriteArrayList<>();
    private final List<Fruit> fruits = new CopyOnWriteArrayList<>();
    private final List<Record<Pet, Person>> pets = new CopyOnWriteArrayList<>();

    @Incoming("people-in")
    public void consume(Person person) {
        people.add(person);
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
