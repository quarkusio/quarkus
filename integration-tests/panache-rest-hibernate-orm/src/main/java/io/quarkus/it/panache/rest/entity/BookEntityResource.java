package io.quarkus.it.panache.rest.entity;

import io.quarkus.panache.rest.common.ResourceProperties;
import io.quarkus.panache.rest.hibernate.orm.PanacheEntityCrudResource;

@ResourceProperties(hal = true)
public interface BookEntityResource extends PanacheEntityCrudResource<BookEntity, Long> {

}
