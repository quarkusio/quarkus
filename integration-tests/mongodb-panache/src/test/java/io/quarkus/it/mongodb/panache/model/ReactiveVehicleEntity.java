package io.quarkus.it.mongodb.panache.model;

import java.util.Objects;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;

public abstract class ReactiveVehicleEntity extends ReactivePanacheMongoEntityBase {

    @BsonId
    public String id;
    public Integer modelYear;

    public ReactiveVehicleEntity() {
    }

    public ReactiveVehicleEntity(String id, Integer modelYear) {
        this.id = id;
        this.modelYear = modelYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ReactiveVehicleEntity))
            return false;
        ReactiveVehicleEntity vehicle = (ReactiveVehicleEntity) o;
        return Objects.equals(id, vehicle.id) && Objects.equals(modelYear, vehicle.modelYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, modelYear);
    }
}
