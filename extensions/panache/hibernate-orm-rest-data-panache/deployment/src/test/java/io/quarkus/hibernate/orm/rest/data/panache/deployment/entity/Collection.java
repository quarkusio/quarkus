package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class Collection extends PanacheEntityBase {

    @Id
    public String id;

    public String name;

    /**
     * This field is used to reproduce the issue: https://github.com/quarkusio/quarkus/issues/30605
     */
    @Column(name = "type", columnDefinition = "int default 100")
    public int type;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "collection")
    public List<Item> items = new LinkedList<>();
}
