package io.quarkus.hibernate.orm.metadatabuildercontributor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class MyEntity {
    @Id
    private long id;

    private String name;

    public MyEntity() {
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
