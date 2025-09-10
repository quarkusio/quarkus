package io.quarkus.it.panache.secondary;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class AnotherFruit extends PanacheEntity {

    public String name;
    public String color;

    public AnotherFruit(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public AnotherFruit() {
    }

}
