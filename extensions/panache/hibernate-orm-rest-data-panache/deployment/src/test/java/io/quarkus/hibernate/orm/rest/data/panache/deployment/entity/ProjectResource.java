package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;

/**
 * Having a path param in the path reproduces the issue of having HAL enabled spites it should be disabled by default.
 */
@ResourceProperties(path = "/{group}/projects")
public interface ProjectResource extends PanacheEntityResource<Project, String> {
}
