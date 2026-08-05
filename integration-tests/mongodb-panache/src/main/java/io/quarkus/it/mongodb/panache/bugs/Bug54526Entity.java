package io.quarkus.it.mongodb.panache.bugs;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = "bug54526")
public class Bug54526Entity extends PanacheMongoEntity {

    public Isbn isbn;
    public String title;
}
