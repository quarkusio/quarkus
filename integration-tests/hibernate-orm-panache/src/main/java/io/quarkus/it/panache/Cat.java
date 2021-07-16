package io.quarkus.it.panache;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Cat extends PanacheEntity {

    String name;

    @ManyToOne
    CatOwner owner;

    public Cat(String name, CatOwner owner) {
        this.name = name;
        this.owner = owner;
    }

    public Cat() {
    }
}
