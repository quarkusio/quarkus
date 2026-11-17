package io.quarkus.it.data.hibernate;

import jakarta.persistence.Entity;

import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;

@Entity
public class Cat extends ManagedEntity {
    public String name;
    public int age;
    public String color;

    public interface Repository extends ManagedRepository<Cat> {
    }
}
