package io.quarkus.it.hibernate.orm.rest.data.panache;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Author extends PanacheEntity {

    public String name;

    public String dob;
}
