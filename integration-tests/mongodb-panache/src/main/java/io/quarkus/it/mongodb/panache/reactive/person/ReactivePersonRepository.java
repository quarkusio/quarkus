package io.quarkus.it.mongodb.panache.reactive.person;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.it.mongodb.panache.person.Person;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;

@ApplicationScoped
public class ReactivePersonRepository implements ReactivePanacheMongoRepositoryBase<Person, Long> {
}
