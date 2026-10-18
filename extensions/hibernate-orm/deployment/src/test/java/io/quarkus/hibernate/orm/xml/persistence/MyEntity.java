package io.quarkus.hibernate.orm.xml.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MyEntity {

    @Id
    public long id;

    public String name;

    public MyEntity() {
    }

    public MyEntity(String name) {
        this.name = name;
    }

}
