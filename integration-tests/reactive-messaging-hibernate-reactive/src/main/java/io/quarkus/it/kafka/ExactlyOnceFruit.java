package io.quarkus.it.kafka;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class ExactlyOnceFruit extends PanacheEntity {

    public String name;

    public ExactlyOnceFruit(String name) {
        this.name = name;
    }

    public ExactlyOnceFruit() {
    }
}
