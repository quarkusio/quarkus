package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class Project extends PanacheEntityBase {

    @Id
    public String name;
}
