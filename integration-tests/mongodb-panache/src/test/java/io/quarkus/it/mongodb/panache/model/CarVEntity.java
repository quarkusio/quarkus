package io.quarkus.it.mongodb.panache.model;

import java.util.Objects;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.common.Version;

@MongoEntity
public class CarVEntity extends VehicleEntity {

    @Version
    public Long version;

    public CarVEntity() {
    }

    public CarVEntity(String id, Integer modelYear, Long version) {
        super(id, modelYear);
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CarVEntity))
            return false;
        if (!super.equals(o))
            return false;
        CarVEntity carV = (CarVEntity) o;
        return Objects.equals(version, carV.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), version);
    }
}
