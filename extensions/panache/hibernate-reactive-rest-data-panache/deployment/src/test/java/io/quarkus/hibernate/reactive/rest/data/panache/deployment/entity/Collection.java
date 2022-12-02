package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
public class Collection extends PanacheEntityBase {

    @Id
    public String id;

    public String name;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "collection")
    public List<Item> items = new LinkedList<>();
}
