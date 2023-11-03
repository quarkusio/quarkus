package io.quarkus.it.mongodb.panache.person;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;

@ApplicationScoped
public class PersonRepository implements PanacheMongoRepositoryBase<Person, Long> {
}
