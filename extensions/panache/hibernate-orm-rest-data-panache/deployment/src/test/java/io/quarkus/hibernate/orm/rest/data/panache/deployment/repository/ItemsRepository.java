package io.quarkus.hibernate.orm.rest.data.panache.deployment.repository;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class ItemsRepository implements PanacheRepository<Item> {
}
