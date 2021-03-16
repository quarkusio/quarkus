package io.quarkus.it.panache;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Cat extends PanacheEntity {

    @ManyToOne
    CatOwner owner;

    public Cat(CatOwner owner) {
        this.owner = owner;
    }

    public Cat() {
    }
}
