package io.quarkus.hibernate.orm.panache.deployment.test.record;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Person extends PanacheEntity {
    public String firstname;
    public String lastname;
    public Status status = Status.ALIVE;
}
