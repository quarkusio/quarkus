package io.quarkus.it.mongodb.discriminator;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@BsonDiscriminator(key = "type", value = "MOTO")
public class Moto extends Vehicle {
    public boolean sideCar;
}
