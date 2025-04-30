package io.quarkus.it.mongodb.panache.record;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class PersonRecordRepository implements PanacheMongoRepository<PersonRecord> {
}
