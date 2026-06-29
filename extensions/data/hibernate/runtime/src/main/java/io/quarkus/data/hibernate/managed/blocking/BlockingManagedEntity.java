package io.quarkus.data.hibernate.managed.blocking;

import jakarta.json.bind.annotation.JsonbTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.data.hibernate.managed.ManagedEntityOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheBlockingOperations;
import io.quarkus.data.hibernate.runtime.spi.PanacheOperations;

public interface BlockingManagedEntity extends ManagedEntityOperations<Void, Boolean> {

    private PanacheBlockingOperations operations() {
        return PanacheOperations.getBlockingManaged();
    }

    @Override
    public default Void persist() {
        return operations().persist(this);
    }

    @Override
    public default Void persistAndFlush() {
        return operations().persistAndFlush(this);
    }

    @Override
    public default Void delete() {
        return operations().delete(this);
    }

    @JsonbTransient
    // @JsonIgnore is here to avoid serialization of this property with jackson
    @JsonIgnore
    @Override
    public default Boolean isPersistent() {
        return operations().isPersistent(this);
    }

}
