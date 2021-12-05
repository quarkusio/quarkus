package io.quarkus.hibernate.orm.panache.deployment.test.inheritance;

import javax.persistence.MappedSuperclass;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@MappedSuperclass
public class MappedParent extends PanacheEntity {
    public String name;
}
