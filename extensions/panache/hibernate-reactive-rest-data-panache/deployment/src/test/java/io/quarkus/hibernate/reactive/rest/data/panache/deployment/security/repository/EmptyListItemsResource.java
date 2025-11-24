package io.quarkus.hibernate.reactive.rest.data.panache.deployment.security.repository;

import io.quarkus.hibernate.reactive.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.EmptyListItem;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository.EmptyListItemsRepository;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.security.PermissionsAllowed;

@PermissionsAllowed("list-empty")
@ResourceProperties(hal = true)
public interface EmptyListItemsResource extends PanacheRepositoryResource<EmptyListItemsRepository, EmptyListItem, Long> {
}
