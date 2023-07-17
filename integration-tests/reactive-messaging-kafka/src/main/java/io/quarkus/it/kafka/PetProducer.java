package io.quarkus.it.kafka;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.kafka.Record;

@ApplicationScoped
public class PetProducer {
    @Outgoing("pets-out")
    public Multi<Record<Pet, Person>> generatePets() {
        return Multi.createFrom().items(
                Record.of(new Pet("dog"), new Person("john")),
                Record.of(new Pet("dog"), new Person("james")),
                Record.of(new Pet("rabbit"), new Person("clement")));
    }
}
