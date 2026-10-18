package io.quarkus.it.jpa.h2;

import jakarta.persistence.Entity;

/**
 * This entity isn't directly referenced: its mere presence is
 * useful to be able to verify bootstrap capabilities in the
 * presence of abstract entities in the hierarchy.
 */
@Entity
public class NormalPointEntity extends PointEntity {

    private String place;

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }
}
