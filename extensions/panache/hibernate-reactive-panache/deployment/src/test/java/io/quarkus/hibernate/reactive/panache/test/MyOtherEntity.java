package io.quarkus.hibernate.reactive.panache.test;

import javax.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class MyOtherEntity extends PanacheEntity {
    public String name;
}
