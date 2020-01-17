package io.quarkus.it.panache.rest.entity;

import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Table(name = "author")
public class AuthorEntity extends PanacheEntity {

    public String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate dob;

    public static AuthorEntity create(Long id, String name, LocalDate dob) {
        AuthorEntity author = new AuthorEntity();
        author.id = id;
        author.name = name;
        author.dob = dob;

        return author;
    }
}
