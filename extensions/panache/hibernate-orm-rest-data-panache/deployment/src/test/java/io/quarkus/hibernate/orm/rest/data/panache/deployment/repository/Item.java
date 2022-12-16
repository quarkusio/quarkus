package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import javax.persistence.Entity;
import javax.persistence.NamedQuery;

@Entity
@NamedQuery(name = "Item.containsInName", query = "from Item where name like CONCAT('%', CONCAT(:name, '%'))")
public class Item extends AbstractItem<Long> {

}
