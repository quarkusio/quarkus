package io.quarkus.it.hibertnate.orm.graphql.panache;

import java.time.LocalDate;

import jakarta.persistence.Entity;

import org.hibernate.annotations.JdbcType;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Author extends PanacheEntity {

    public String name;
    @JdbcType(LocalDateJdbcType.class)
    public LocalDate dob;
}
