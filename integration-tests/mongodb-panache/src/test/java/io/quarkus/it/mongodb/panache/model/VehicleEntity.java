package io.quarkus.it.mongodb.panache.model;

import java.util.Objects;

import org.bson.codecs.pojo.annotations.BsonId;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;

public abstract class VehicleEntity extends PanacheMongoEntityBase {

    @BsonId
    public String id;
    public Integer modelYear;

    public VehicleEntity() {
    }

    public VehicleEntity(String id, Integer modelYear) {
        this.id = id;
        this.modelYear = modelYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof VehicleEntity))
            return false;
        VehicleEntity vehicle = (VehicleEntity) o;
        return Objects.equals(id, vehicle.id) && Objects.equals(modelYear, vehicle.modelYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, modelYear);
    }
}
