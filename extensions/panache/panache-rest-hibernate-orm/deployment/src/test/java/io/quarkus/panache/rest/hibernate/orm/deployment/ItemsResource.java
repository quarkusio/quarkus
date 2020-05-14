package io.quarkus.panache.rest.hibernate.orm.deployment;

import io.quarkus.panache.rest.common.ResourceProperties;
import io.quarkus.panache.rest.hibernate.orm.PanacheEntityCrudResource;

@ResourceProperties(hal = true)
public interface ItemsResource extends PanacheEntityCrudResource<Item, Long> {
}
