package io.quarkus.it.panache.reactive;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
@NamedQuery(name = "NamedQueryWith2QueriesEntity.getAll1", query = "from NamedQueryWith2QueriesEntity")
@NamedQuery(name = "NamedQueryWith2QueriesEntity.getAll2", query = "from NamedQueryWith2QueriesEntity")
public class NamedQueryWith2QueriesEntity extends PanacheEntity {
    public String test;
}
