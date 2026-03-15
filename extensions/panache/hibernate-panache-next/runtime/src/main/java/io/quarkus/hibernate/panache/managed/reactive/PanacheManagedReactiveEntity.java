package io.quarkus.hibernate.panache.managed.reactive;

import jakarta.json.bind.annotation.JsonbTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.panache.managed.PanacheManagedEntityOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheOperations;
import io.quarkus.hibernate.panache.runtime.spi.PanacheReactiveOperations;
import io.smallrye.mutiny.Uni;

public interface PanacheManagedReactiveEntity extends PanacheManagedEntityOperations<Uni<Void>, Uni<Boolean>> {

    private PanacheReactiveOperations operations() {
        return PanacheOperations.getReactiveManaged();
    }

    @Override
    public default Uni<Void> persist() {
        return operations().persist(this);
    }

    @Override
    public default Uni<Void> persistAndFlush() {
        return operations().persistAndFlush(this);
    }

    @Override
    public default Uni<Void> delete() {
        return operations().delete(this);
    }

    @JsonbTransient
    // @JsonIgnore is here to avoid serialization of this property with jackson
    @JsonIgnore
    @Override
    public default Uni<Boolean> isPersistent() {
        return operations().isPersistent(this);
    }

}
