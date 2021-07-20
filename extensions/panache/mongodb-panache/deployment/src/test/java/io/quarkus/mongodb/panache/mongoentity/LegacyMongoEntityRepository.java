package io.quarkus.mongodb.panache.mongoentity;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class LegacyMongoEntityRepository implements PanacheMongoRepository<LegacyMongoEntityEntity> {
}
