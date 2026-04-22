package io.quarkus.hibernate.orm.enums;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class MyEntityWithEnum {

    private long id;
    private String name;
    private Status status;

    public MyEntityWithEnum() {
    }

    public MyEntityWithEnum(String name, Status status) {
        this.name = name;
        this.status = status;
    }

    @Id
    @GeneratedValue
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
