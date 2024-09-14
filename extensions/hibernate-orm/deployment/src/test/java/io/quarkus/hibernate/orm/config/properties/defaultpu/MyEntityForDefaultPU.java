package io.quarkus.hibernate.orm.config.properties.defaultpu;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MyEntityForDefaultPU {
    @Id
    private long id;

    private String name;

    public MyEntityForDefaultPU() {
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
