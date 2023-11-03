package io.quarkus.it.spring.data.jpa;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class NamedEntity {

    private String name;

    public NamedEntity() {
    }

    public NamedEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
