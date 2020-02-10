package io.quarkus.it.mongodb.discriminator;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@BsonDiscriminator(key = "type", value = "CAR")
public class Car extends Vehicle {
    public int seatNumber;
}
