package io.quarkus.it.mongodb.panache.model;

import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity
public class ReactiveCarEntity extends ReactiveVehicleEntity {

    public ReactiveCarEntity() {
    }

    public ReactiveCarEntity(String id, Integer modelYear) {
        super(id, modelYear);
    }
}
