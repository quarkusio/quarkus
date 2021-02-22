package io.quarkus.it.panache;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class Book extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Integer id;

    public String name;

    @JsonIgnore
    public String author;

    public Book(String name, String author) {
        this.name = name;
        this.author = author;
    }

    public Book() {
    }
}
