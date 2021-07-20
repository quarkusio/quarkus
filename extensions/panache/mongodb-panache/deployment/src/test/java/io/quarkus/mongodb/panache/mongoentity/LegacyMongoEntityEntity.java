package io.quarkus.mongodb.panache.mongoentity;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;

@MongoEntity(database = "legacyMongoEntity")
public class LegacyMongoEntityEntity extends PanacheMongoEntity {
    public String field;
}
