package io.quarkus.it.panache;

import javax.persistence.Entity;
import javax.persistence.NamedQuery;

@Entity
@NamedQuery(name = "NamedQueryEntity.getAll", query = "from NamedQueryEntity")
public class NamedQueryEntity extends NamedQueryMappedSuperClass {
    public String test;
}
