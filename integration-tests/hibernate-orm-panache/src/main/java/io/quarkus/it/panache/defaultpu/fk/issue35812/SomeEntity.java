package io.quarkus.it.panache.defaultpu.fk.issue35812;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class SomeEntity extends PanacheEntity {
    public String string;
}
