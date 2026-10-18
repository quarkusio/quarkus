package io.quarkus.it.panache.reactive;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class CatOwner extends PanacheEntity {

    public String name;

    public CatOwner(String name) {
        this.name = name;
    }

    public CatOwner() {
    }
}
