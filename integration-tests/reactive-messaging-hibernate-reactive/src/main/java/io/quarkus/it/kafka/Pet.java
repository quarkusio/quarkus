package io.quarkus.it.kafka;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class Pet extends PanacheEntity {

    public String name;

    public Pet(String name) {
        this.name = name;
    }

    public Pet() {
        // Jackson will use this constructor.
    }
}
