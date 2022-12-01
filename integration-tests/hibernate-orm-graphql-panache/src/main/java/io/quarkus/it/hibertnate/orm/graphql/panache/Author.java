package io.quarkus.it.hibertnate.orm.graphql.panache;

import java.time.LocalDate;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Author extends PanacheEntity {

    public String name;
    public LocalDate dob;
}
