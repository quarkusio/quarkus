package io.quarkus.context.test.mutiny;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class SomeEntity extends PanacheEntity {

    public String name;

}
