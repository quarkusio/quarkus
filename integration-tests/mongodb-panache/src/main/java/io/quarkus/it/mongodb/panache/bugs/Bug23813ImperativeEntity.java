package io.quarkus.it.mongodb.panache.bugs;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "TheBug23813ImperativeEntity", database = "Bug23813ImperativeEntity")
public class Bug23813ImperativeEntity extends PanacheMongoEntity {
    public String field;
}
