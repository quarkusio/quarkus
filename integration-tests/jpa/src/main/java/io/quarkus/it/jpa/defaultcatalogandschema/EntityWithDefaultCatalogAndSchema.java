package io.quarkus.it.jpa.defaultcatalogandschema;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity(name = EntityWithDefaultCatalogAndSchema.NAME)
public class EntityWithDefaultCatalogAndSchema {
    public static final String NAME = "ent_with_defaults";

    @Id
    @GeneratedValue
    public Long id;

    @Basic
    public String basic;
}
