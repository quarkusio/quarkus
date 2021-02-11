package io.quarkus.it.mongodb.discriminator;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@BsonDiscriminator(key = "type")
public abstract class Vehicle {
    public String type;
    public String name;
}
