package io.quarkus.it.mongodb.panache.model;

import java.util.Objects;

import org.bson.codecs.pojo.annotations.BsonId;

public abstract class Vehicle {

    @BsonId
    public String id;
    public Integer modelYear;

    public Vehicle() {
    }

    public Vehicle(String id, Integer modelYear) {
        this.id = id;
        this.modelYear = modelYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Vehicle))
            return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(id, vehicle.id) && Objects.equals(modelYear, vehicle.modelYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, modelYear);
    }
}
