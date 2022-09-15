package io.quarkus.mongodb.panache.mongoentity;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class MongoEntityRepository implements PanacheMongoRepository<MongoEntityEntity> {
}
