package io.quarkus.hibernate.reactive.rest.data.panache.deployment.security.entity;

import io.quarkus.hibernate.reactive.rest.data.panache.PanacheEntityResource;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.EmptyListItem;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.security.PermissionsAllowed;

@PermissionsAllowed("list-empty")
@ResourceProperties(hal = true)
public interface EmptyListItemsResource extends PanacheEntityResource<EmptyListItem, Long> {
}
