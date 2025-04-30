package io.quarkus.hibernate.orm.config.properties.overridespu;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MyEntityForOverridesPU {
    @Id
    private long id;

    private String name;

    public MyEntityForOverridesPU() {
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
