package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true)
public interface ItemsResource extends PanacheRepositoryResource<ItemsRepository, Item, Long> {
}
