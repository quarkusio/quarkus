package io.quarkus.it.mongodb.panache.bugs;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

@MongoEntity(collection = "TheBug23813ReactiveEntity", database = "Bug23813ReactiveEntity")
public class Bug23813ReactiveEntity extends ReactivePanacheMongoEntity {
    public String field;
}
