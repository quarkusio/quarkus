package io.quarkus.it.mongodb.pojo;

import java.util.Optional;

import org.bson.types.ObjectId;

public class Pojo {
    public ObjectId id;
    public String description;
    public Optional<String> optionalString;
}
