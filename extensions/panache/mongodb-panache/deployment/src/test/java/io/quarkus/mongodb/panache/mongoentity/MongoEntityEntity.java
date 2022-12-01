package io.quarkus.mongodb.panache.mongoentity;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(database = "mongoEntity")
public class MongoEntityEntity extends PanacheMongoEntity {
    public String field;
}
