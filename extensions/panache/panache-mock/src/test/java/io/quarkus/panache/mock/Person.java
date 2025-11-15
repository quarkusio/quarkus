package io.quarkus.panache.mock;

import java.util.List;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Person extends PanacheEntity {

    public String name;

    public static List<Person> findOrdered() {
        return find("ORDER BY name").list();
    }
}
