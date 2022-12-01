package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import io.quarkus.hibernate.reactive.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true)
public interface EmptyListItemsResource extends PanacheEntityResource<EmptyListItem, Long> {
}
