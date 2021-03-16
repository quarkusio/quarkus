package io.quarkus.it.panache.reactive;

import javax.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class Fruit extends PanacheEntity {

    public String name;
    public String color;

    public Fruit(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public Fruit() {
    }

}
