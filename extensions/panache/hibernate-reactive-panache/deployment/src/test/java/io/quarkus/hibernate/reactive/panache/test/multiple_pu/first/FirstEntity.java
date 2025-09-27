package io.quarkus.hibernate.reactive.panache.test.multiple_pu.first;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class FirstEntity extends PanacheEntity {

    public String name;
}
