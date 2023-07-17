package io.quarkus.it.jpa.h2;

import jakarta.persistence.Entity;

/**
 * This entity isn't directly referenced: its mere presence is
 * useful to be able to verify bootstrap capabilities in the
 * presence of abstract entities in the hierarchy.
 */
@Entity
public abstract class PointEntity extends DataIdentity {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
