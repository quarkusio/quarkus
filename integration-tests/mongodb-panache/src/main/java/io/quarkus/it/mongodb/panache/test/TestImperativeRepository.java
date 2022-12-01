package io.quarkus.it.mongodb.panache.test;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class TestImperativeRepository implements PanacheMongoRepository<TestImperativeEntity> {
}
