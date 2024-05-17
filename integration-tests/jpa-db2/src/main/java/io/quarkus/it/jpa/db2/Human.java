package io.quarkus.it.jpa.db2;

import jakarta.persistence.MappedSuperclass;

/**
 * Mapped superclass test
 */
@MappedSuperclass
public class Human extends Animal {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
