package io.quarkus.it.panache;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Cat extends PanacheEntity {

    String name;

    @ManyToOne
    CatOwner owner;

    Double weight;

    public Cat(String name, CatOwner owner) {
        this.name = name;
        this.owner = owner;
    }

    public Cat() {
    }
}
