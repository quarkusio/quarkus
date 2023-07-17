package io.quarkus.it.mongodb.panache.test;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;

@ApplicationScoped
public class TestReactiveRepository implements ReactivePanacheMongoRepository<TestReactiveEntity> {
}
