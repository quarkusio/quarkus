package io.quarkus.it.mongodb.panache.model;

import java.util.Objects;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.common.Version;

@MongoEntity
public class CarV extends Vehicle {

    @Version
    public Long version;

    public CarV() {
    }

    public CarV(String id, Integer modelYear, Long version)

    {
        super(id, modelYear);
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CarV))
            return false;
        if (!super.equals(o))
            return false;
        CarV carV = (CarV) o;
        return Objects.equals(version, carV.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), version);
    }

    @Override
    public String toString() {
        return "CarV{" +
                "version=" + version +
                ", id='" + id + '\'' +
                ", modelYear=" + modelYear +
                '}';
    }
}
