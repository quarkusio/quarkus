package io.quarkus.hibernate.reactive.panache.test;

import javax.persistence.Id;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

public class DuplicateIdEntity extends PanacheEntity {
    @Id
    public String customId;
}
