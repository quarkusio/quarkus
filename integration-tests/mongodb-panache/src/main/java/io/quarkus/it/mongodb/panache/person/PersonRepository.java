package io.quarkus.it.mongodb.panache.person;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;

@ApplicationScoped
public class PersonRepository implements PanacheMongoRepositoryBase<Person, Long> {
}
