package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import io.quarkus.hibernate.reactive.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true, paged = false, halCollectionName = "item-collections")
public interface CollectionsResource extends PanacheRepositoryResource<CollectionsRepository, Collection, String> {
}
