package io.quarkus.panache.rest.hibernate.orm.deployment;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Item extends PanacheEntity {

    public String value;
}
