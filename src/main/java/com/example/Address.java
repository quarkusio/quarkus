package com.example;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Address {

    private long id;
    private String name;

    Address() {
    }

    public Address(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Id
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
