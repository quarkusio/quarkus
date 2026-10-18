package io.quarkus.hibernate.reactive.panache.test.multiple_pu.first;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class FirstEntity extends PanacheEntity {

    public String name;

    // You need getter and setter otherwise dirty check won't be enabled here
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
