package io.quarkus.it.kafka;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkus.it.kafka.fruit.Fruit;
import io.quarkus.it.kafka.fruit.FruitDto;
import io.quarkus.it.kafka.people.Person;
import io.quarkus.it.kafka.pet.Pet;

@ApplicationScoped
public class KafkaReceivers {

    private final List<Person> people = new CopyOnWriteArrayList<>();

    private final List<Pet> pets = new CopyOnWriteArrayList<>();

    @Channel("fruits-persisted")
    Emitter<FruitDto> emitter;

    @Incoming("fruits-in")
    @Transactional
    public CompletionStage<Void> persist(Fruit fruit) {
        fruit.persist();
        return emitter.send(new FruitDto(fruit));
    }

    @Incoming("pets-in")
    public void persist(Pet pet) {
        pets.add(pet);
    }

    public List<Fruit> getFruits() {
        return Fruit.listAll();
    }

    public List<Person> getPeople() {
        return people;
    }

    public List<Pet> getPets() {
        return Pet.listAll();
    }

    public List<Pet> getPetsConsumed() {
        return pets;
    }

}
