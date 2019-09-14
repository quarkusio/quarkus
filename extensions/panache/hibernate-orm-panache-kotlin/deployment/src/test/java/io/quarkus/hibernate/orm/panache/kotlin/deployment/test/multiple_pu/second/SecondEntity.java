package io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.second;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity;

@Entity
public class SecondEntity extends PanacheEntity {

    public String name;
}
