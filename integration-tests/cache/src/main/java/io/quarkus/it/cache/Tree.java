package io.quarkus.it.cache;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Cacheable
public class Tree extends PanacheEntity {

    @Column(unique = true)
    public String name;
}
