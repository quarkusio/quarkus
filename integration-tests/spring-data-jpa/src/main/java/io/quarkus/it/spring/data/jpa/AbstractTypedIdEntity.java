package io.quarkus.it.spring.data.jpa;

import java.io.Serializable;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

// example "base entity" using strongly typed ids (instead of just long)
@MappedSuperclass
public abstract class AbstractTypedIdEntity<ID extends Serializable> {

    // Using package visibility instead of private visibility
    // in order to work around https://hibernate.atlassian.net/browse/HHH-16832
    @EmbeddedId
    ID id;

    protected AbstractTypedIdEntity(ID id) {
        this.id = id;
    }

    public ID getId() {
        return id;
    }
}
