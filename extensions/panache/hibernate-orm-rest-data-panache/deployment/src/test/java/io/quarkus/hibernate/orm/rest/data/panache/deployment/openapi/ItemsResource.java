package io.quarkus.hibernate.orm.rest.data.panache.deployment.openapi;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true)
public interface ItemsResource extends PanacheRepositoryResource<ItemsRepository, Item, Long> {
}
