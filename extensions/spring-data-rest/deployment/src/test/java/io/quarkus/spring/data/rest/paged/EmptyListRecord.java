package io.quarkus.spring.data.rest.paged;

import jakarta.persistence.Entity;

@Entity
public class EmptyListRecord extends AbstractEntity<Long> {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
