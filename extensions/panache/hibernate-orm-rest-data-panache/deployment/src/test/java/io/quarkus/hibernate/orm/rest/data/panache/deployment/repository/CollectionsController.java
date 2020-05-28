package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true, paged = false)
public interface CollectionsController extends PanacheRepositoryResource<CollectionsRepository, Collection, String> {
}
