package io.quarkus.it.cache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Cacheable
public class Tree extends PanacheEntity {

    @Column(unique = true)
    public String name;
}
