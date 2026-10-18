package io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.second;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class SecondEntity extends PanacheEntity {

    public String name;
}
