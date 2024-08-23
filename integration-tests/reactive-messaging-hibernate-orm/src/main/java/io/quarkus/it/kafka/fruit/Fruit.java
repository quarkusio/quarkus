package io.quarkus.it.kafka.fruit;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

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
