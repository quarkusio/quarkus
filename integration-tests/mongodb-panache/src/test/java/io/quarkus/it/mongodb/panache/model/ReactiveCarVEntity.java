package io.quarkus.it.mongodb.panache.model;

import java.util.Objects;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.common.Version;

@MongoEntity
public class ReactiveCarVEntity extends ReactiveVehicleEntity {

    @Version
    public Long version;

    public ReactiveCarVEntity() {
    }

    public ReactiveCarVEntity(String id, Integer modelYear, Long version) {
        super(id, modelYear);
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ReactiveCarVEntity))
            return false;
        if (!super.equals(o))
            return false;
        ReactiveCarVEntity carV = (ReactiveCarVEntity) o;
        return Objects.equals(version, carV.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), version);
    }
}
