package io.quarkus.hibernate.orm.panache.test;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class MyEntity extends PanacheEntity {
    public String name;

    @Override
    public EntityManager entityManager() {
        return super.entityManager();
    }
}
