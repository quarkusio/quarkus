package io.quarkus.it.jpa.defaultcatalogandschema;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity(name = EntityWithDefaultCatalogAndSchema.NAME)
public class EntityWithDefaultCatalogAndSchema {
    public static final String NAME = "ent_with_defaults";

    @Id
    @GeneratedValue
    public Long id;

    @Basic
    public String basic;
}
