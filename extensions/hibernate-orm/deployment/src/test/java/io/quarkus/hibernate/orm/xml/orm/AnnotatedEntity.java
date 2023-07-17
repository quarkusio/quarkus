package io.quarkus.hibernate.orm.xml.orm;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class AnnotatedEntity {

    @Id
    @GeneratedValue
    private long id;

    @Basic
    private String name;

    public AnnotatedEntity() {
    }

    public AnnotatedEntity(String name) {
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
