package io.quarkus.it.panache.rest.entity;

import io.quarkus.panache.rest.common.PanacheRestResource;
import io.quarkus.panache.rest.hibernate.orm.PanacheEntityCrudResource;

@PanacheRestResource(hal = true)
public interface BookEntityCrudResource extends PanacheEntityCrudResource<BookEntity, Long> {

}
