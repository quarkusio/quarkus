package io.quarkus.hibernate.orm.packages;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.Filter;

@Entity
@Filter(name = "filter")
public class ParentEntity {

    @Id
    @GeneratedValue
    private long id;

    private String name;

    public ParentEntity() {
    }

    public ParentEntity(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ParentEntity:" + name;
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
