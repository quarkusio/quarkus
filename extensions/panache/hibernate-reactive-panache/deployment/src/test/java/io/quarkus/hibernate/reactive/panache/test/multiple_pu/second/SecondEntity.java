package io.quarkus.hibernate.reactive.panache.test.multiple_pu.second;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class SecondEntity extends PanacheEntity {

    public String name;
}
