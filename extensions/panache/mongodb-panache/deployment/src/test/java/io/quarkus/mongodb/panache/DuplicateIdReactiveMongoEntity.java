package io.quarkus.mongodb.panache;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

public class DuplicateIdReactiveMongoEntity extends ReactivePanacheMongoEntity {
    @BsonId
    public String customId;
}
