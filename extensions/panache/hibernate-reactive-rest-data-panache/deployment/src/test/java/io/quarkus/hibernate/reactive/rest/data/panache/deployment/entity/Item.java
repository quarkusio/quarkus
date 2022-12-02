package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import javax.persistence.Entity;
import javax.persistence.NamedQuery;

@Entity
@NamedQuery(name = "Item.containsInName", query = "from Item where name like CONCAT('%', CONCAT(:name, '%'))")
public class Item extends AbstractItem<Long> {

}
