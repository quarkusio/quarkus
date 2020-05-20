package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true)
public interface ItemsResource extends PanacheEntityResource<Item, Long> {
}
