package io.quarkus.it.panache.hibernate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class HibernateBook extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Integer id;

    public String name;

    public String author;

    public HibernateBook(String name, String author) {
        this.name = name;
        this.author = author;
    }

    public HibernateBook() {
    }

}
