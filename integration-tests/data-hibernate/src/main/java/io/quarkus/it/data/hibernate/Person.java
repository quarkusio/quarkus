package io.quarkus.it.data.hibernate;

import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;

@Entity
public class Person extends ManagedEntity {
    public String name;
    public int age;

    // Nested repository interface - Panache Next pattern
    public interface Repository extends ManagedRepository<Person> {
    }
}
