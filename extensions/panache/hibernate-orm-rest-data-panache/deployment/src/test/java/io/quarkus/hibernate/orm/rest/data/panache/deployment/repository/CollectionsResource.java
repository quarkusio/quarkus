package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true, paged = false, halCollectionName = "item-collections", rolesAllowed = "user")
public interface CollectionsResource extends PanacheRepositoryResource<CollectionsRepository, Collection, String> {
    @MethodProperties(rolesAllowed = "admin")
    boolean delete(String name);
}
