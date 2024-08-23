package io.quarkus.spring.data.rest.crud;

import jakarta.persistence.Entity;

import io.quarkus.spring.data.rest.AbstractEntity;

@Entity
public class Record extends AbstractEntity<Long> {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
