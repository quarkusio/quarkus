package io.quarkus.it.spring.data.jpa;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class Animal {

    @Id
    @GeneratedValue
    private long id;

    public long getId() {
        return id;
    }
}
