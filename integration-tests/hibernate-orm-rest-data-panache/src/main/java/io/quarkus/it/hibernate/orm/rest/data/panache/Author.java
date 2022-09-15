package io.quarkus.it.hibernate.orm.rest.data.panache;

import java.time.LocalDate;

import jakarta.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Author extends PanacheEntity {

    public String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate dob;
}
