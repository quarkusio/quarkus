package io.quarkus.context.test;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class ContextEntity extends PanacheEntity {

    public String name;

}
