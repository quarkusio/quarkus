package io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.first;

import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity;

@Entity
public class FirstEntity extends PanacheEntity {

    public String name;
}
