package io.quarkus.hibernate.reactive.rest.data.panache.deployment.build;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
public class Collection extends PanacheEntityBase {

    @Id
    public String id;

    public String name;
}
