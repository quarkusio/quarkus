package io.quarkus.it.panache.custompu;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class CustomPuEntity extends PanacheEntity {
    public String string;
}
