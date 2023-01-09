package io.quarkus.it.hibernate.panache.person;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Person extends PanacheEntity {
    public String firstname;
    public String lastname;
    public Status status = Status.ALIVE;
}
