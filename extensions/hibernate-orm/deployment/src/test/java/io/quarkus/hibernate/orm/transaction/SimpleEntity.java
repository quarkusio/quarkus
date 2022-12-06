package io.quarkus.hibernate.orm.transaction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SimpleEntity {

    @Id
    private long id;

    private String name;

    public SimpleEntity() {
    }

    public SimpleEntity(String name) {
        this.name = name;
    }

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
