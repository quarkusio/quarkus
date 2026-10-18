package io.quarkus.it.panache.reactive;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@NamedQuery(name = "Cat.NameAndOwnerName", query = "select c.name, c.owner.name as ownerName from Cat c where c.name = :name")
@Entity
public class Cat extends PanacheEntity {

    String name;
    @ManyToOne
    CatOwner owner;

    Double weight;

    public Cat(CatOwner owner) {
        this(null, owner, null);
    }

    public Cat(String name, CatOwner owner) {
        this(name, owner, null);
    }

    public Cat(String name, CatOwner owner, Double weight) {
        this.name = name;
        this.owner = owner;
        this.weight = weight;
    }

    public Cat() {
    }
}
