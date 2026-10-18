package io.quarkus.it.mongodb.panache.test;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class TestImperativeRepository implements PanacheMongoRepository<TestImperativeEntity> {
}
