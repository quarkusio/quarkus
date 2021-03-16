package io.quarkus.it.panache;

import javax.persistence.MappedSuperclass;
import javax.persistence.NamedQuery;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@MappedSuperclass
@NamedQuery(name = "NamedQueryMappedSuperClass.getAll", query = "from NamedQueryEntity")
public class NamedQueryMappedSuperClass extends PanacheEntity {
    public String superField;
}
