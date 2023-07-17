package io.quarkus.it.panache.reactive;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedQuery;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@MappedSuperclass
@NamedQuery(name = "NamedQueryMappedSuperClass.getAll", query = "from NamedQueryEntity")
public class NamedQueryMappedSuperClass extends PanacheEntity {
    public String superField;
}
