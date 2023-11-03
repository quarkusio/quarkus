package io.quarkus.it.panache.reactive;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;

@ApplicationScoped
public class NamedQueryWith2QueriesRepository implements PanacheRepository<NamedQueryWith2QueriesEntity> {
}
