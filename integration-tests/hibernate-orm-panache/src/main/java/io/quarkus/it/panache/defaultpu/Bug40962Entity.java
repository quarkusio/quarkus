package io.quarkus.it.panache.defaultpu;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Bug40962Entity extends PanacheEntity {
    public String name;
    public String location;
}
