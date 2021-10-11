package io.quarkus.it.rabbitmq;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class PeopleProducer {
    @Outgoing("people-out")
    public Multi<Person> generatePeople() {
        return Multi.createFrom().items(
                new Person("bob"),
                new Person("alice"),
                new Person("tom"),
                new Person("jerry"),
                new Person("anna"),
                new Person("ken"));
    }
}
