package io.quarkus.it.panache.next;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.panache.PanacheEntity;
import io.quarkus.hibernate.panache.PanacheRepository;

@Entity
public class Person extends PanacheEntity {
    public String name;
    public int age;

    // Nested repository interface - Panache Next pattern
    public interface Repository extends PanacheRepository<Person> {
    }
}
