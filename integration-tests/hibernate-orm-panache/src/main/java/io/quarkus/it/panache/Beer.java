package io.quarkus.it.panache;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Beer extends PanacheEntity {

    public String name;
}
