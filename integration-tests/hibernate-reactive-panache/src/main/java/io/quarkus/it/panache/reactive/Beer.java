package io.quarkus.it.panache.reactive;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class Beer extends PanacheEntity {

    public String name;
}
