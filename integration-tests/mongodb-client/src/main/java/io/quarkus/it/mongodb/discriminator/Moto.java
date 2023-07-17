package io.quarkus.it.mongodb.discriminator;

import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@BsonDiscriminator(key = "type", value = "MOTO")
public class Moto extends Vehicle {

    private boolean sideCar;

    public Moto() {
    }

    public Moto(String type, String name, boolean sideCar) {
        super(type, name);
        this.sideCar = sideCar;
    }

    public boolean isSideCar() {
        return sideCar;
    }

    public void setSideCar(boolean sideCar) {
        this.sideCar = sideCar;
    }
}
