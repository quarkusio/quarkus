package io.quarkus.it.mongodb.panache.model;

import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity
public class Car extends Vehicle {

    public Car() {
    }

    public Car(String id, Integer modelYear) {
        super(id, modelYear);
    }
}
