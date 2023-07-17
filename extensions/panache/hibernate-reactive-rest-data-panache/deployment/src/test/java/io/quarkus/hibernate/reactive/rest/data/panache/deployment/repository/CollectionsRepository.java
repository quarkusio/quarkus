package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

@ApplicationScoped
public class CollectionsRepository implements PanacheRepositoryBase<Collection, String> {
}
