package io.quarkus.it.panache.defaultpu;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@NamedQuery(name = "Cat.NameAndOwnerName", query = "select c.name, c.owner.name as ownerName from Cat c")
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
