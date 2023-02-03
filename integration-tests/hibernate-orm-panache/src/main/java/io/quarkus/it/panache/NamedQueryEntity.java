package io.quarkus.it.panache;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQuery(name = "NamedQueryEntity.getAll", query = "from NamedQueryEntity")
public class NamedQueryEntity extends NamedQueryMappedSuperClass {
    public String test;
}
