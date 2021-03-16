package io.quarkus.it.mongodb.panache.test;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;

@ApplicationScoped
public class TestReactiveRepository implements ReactivePanacheMongoRepository<TestReactiveEntity> {
}
