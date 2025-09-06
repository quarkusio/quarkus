package io.quarkus.it.liquibase.mongodb;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(clientName = "users", collection = "Users")
public class User extends PanacheMongoEntity {
    public String name;
    public String email;
}
