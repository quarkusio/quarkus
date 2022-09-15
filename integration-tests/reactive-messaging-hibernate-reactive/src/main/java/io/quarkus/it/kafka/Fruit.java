package io.quarkus.it.kafka;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class Fruit extends PanacheEntity {

    public String name;

    public Fruit(String name) {
        this.name = name;
    }

    public Fruit() {
        // Jackson will use this constructor
    }
}
