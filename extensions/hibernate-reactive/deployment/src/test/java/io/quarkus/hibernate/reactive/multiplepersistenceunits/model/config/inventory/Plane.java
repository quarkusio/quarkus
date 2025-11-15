package io.quarkus.hibernate.reactive.multiplepersistenceunits.model.config.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Plane {

    private long id;

    private String name;

    public Plane() {
    }

    public Plane(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue
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

    @Override
    public String toString() {
        return "Plane:" + name;
    }
}
