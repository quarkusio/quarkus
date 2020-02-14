package io.quarkus.it.mongodb.panache.axle.person;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.it.mongodb.panache.person.Person;
import io.quarkus.mongodb.panache.axle.ReactivePanacheMongoRepositoryBase;

@ApplicationScoped
public class ReactivePersonRepository implements ReactivePanacheMongoRepositoryBase<Person, Long> {
}
