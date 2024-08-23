package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class CollectionsRepository implements PanacheRepositoryBase<Collection, String> {
}
