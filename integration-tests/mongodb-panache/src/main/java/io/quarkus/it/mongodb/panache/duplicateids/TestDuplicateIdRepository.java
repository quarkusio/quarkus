package io.quarkus.it.mongodb.panache.duplicateids;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;

@ApplicationScoped
public class TestDuplicateIdRepository implements ReactivePanacheMongoRepository<TestDuplicateIdEntity> {
}
