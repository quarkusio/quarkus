package io.quarkus.hibernate.orm.packages;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class ChildEntity1 {

    @Id
    @GeneratedValue
    private long id;

    private String name;

    public ChildEntity1() {
    }

    @Override
    public String toString() {
        return "ChildEntity1:" + name;
    }

    public ChildEntity1(String name) {
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
