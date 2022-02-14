package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;

@ApplicationScoped
public class EmptyListItemsRepository implements PanacheRepository<EmptyListItem> {
}
