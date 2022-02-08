package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import io.quarkus.hibernate.reactive.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true, paged = false, halCollectionName = "item-collections")
public interface CollectionsResource extends PanacheEntityResource<Collection, String> {
}
