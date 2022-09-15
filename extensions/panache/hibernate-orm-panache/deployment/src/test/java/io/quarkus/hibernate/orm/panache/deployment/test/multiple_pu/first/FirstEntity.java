package io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.first;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class FirstEntity extends PanacheEntity {

    public String name;
}
