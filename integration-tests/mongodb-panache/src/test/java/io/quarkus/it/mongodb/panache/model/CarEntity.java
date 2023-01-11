package io.quarkus.it.mongodb.panache.model;

import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity
public class CarEntity extends VehicleEntity {

    public CarEntity() {
    }

    public CarEntity(String id, Integer modelYear) {
        super(id, modelYear);
    }
}
