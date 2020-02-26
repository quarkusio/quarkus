package io.quarkus.it.mongodb.panache.bugs;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.PanacheMongoEntity;

public class LinkedEntity extends PanacheMongoEntity {
    public String name;
    public ObjectId myForeignId;
}
