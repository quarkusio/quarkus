package io.quarkus.it.qute;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Beer extends PanacheEntity {

    public String name;

    public Boolean completed;

    public boolean done;

}
