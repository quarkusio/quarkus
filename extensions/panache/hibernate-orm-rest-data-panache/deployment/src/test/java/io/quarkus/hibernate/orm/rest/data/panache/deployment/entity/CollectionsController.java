package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true, paged = false)
public interface CollectionsController extends PanacheEntityResource<Collection, String> {
}
