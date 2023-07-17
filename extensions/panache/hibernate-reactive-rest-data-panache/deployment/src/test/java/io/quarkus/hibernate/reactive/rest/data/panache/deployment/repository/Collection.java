package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

@Entity
public class Collection extends PanacheEntityBase {

    @Id
    public String id;

    public String name;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "collection")
    public List<Item> items = new LinkedList<>();
}
