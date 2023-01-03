package io.quarkus.it.mongodb.panache.model;

import java.util.Objects;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.common.Version;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

@MongoEntity
public class ReactiveBikeVEntity extends ReactivePanacheMongoEntity {

    @Version
    public Long version;

    public ReactiveBikeVEntity() {
    }

    public ReactiveBikeVEntity(ObjectId id, Long version) {
        this.version = version;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ReactiveBikeVEntity))
            return false;
        ReactiveBikeVEntity bikeV = (ReactiveBikeVEntity) o;

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
