package io.quarkus.it.mongodb.panache.model;

import java.util.Objects;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.common.Version;

@MongoEntity
public class BikeVEntity extends PanacheMongoEntity {

    @Version
    public Long version;

    public BikeVEntity() {
    }

    public BikeVEntity(ObjectId id, Long version) {
        this.version = version;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BikeVEntity))
            return false;
        BikeVEntity bikeV = (BikeVEntity) o;

        return Objects.equals(version, bikeV.version)
                && Objects.equals(id, bikeV.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, id);
    }

    @Override
    public String toString() {
        return "BikeV{" +
                "version=" + version +
                ", id=" + id +
                '}';
    }
}
