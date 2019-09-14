package io.quarkus.hibernate.orm.panache.test.multiple_pu;

import javax.persistence.Entity;
import javax.persistence.PersistenceContext;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@PersistenceContext(unitName = "panache-first-pu")
public class EntityWithFirstPU extends PanacheEntity {

    public String name;
}
