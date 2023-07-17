package io.quarkus.hibernate.orm.panache.deployment.test;

import jakarta.persistence.Id;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

public class DuplicateIdEntity extends PanacheEntity {
    @Id
    public String customId;
}
