package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Item extends PanacheEntity {

    public String value;
}
