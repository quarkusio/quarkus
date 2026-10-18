package io.quarkus.it.liquibase.mongodb;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(clientName = "fruit-client")
public class Fruit extends PanacheMongoEntity {
    public String name;
    public String color;
}
