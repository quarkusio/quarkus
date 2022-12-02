package io.quarkus.it.jpa.db2;

import javax.persistence.MappedSuperclass;

/**
 * Mapped superclass test
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
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
