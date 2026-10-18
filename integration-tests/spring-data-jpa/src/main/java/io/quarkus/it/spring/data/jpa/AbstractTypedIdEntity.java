package io.quarkus.it.spring.data.jpa;

import java.io.Serializable;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

// example "base entity" using strongly typed ids (instead of just long)
@MappedSuperclass
public abstract class AbstractTypedIdEntity<ID extends Serializable> {

    @EmbeddedId
    private ID id;

    protected AbstractTypedIdEntity(ID id) {
        this.id = id;
    }

    public ID getId() {
        return id;
    }
}
