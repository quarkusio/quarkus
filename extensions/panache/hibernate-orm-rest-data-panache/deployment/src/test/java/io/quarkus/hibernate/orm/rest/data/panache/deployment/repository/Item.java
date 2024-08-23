package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQuery(name = "Item.containsInName", query = "from Item where name like CONCAT('%', CONCAT(:name, '%'))")
public class Item extends AbstractItem<Long> {

}
