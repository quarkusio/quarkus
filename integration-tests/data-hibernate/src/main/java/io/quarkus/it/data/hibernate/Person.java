package io.quarkus.it.data.hibernate;

import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.PanacheEntity;
import io.quarkus.data.hibernate.PanacheRepository;

@Entity
public class Person extends PanacheEntity {
    public String name;
    public int age;

    // Nested repository interface - Panache Next pattern
    public interface Repository extends PanacheRepository<Person> {
    }
}
