package io.quarkus.it.panache.reactive;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;

@ApplicationScoped
public class NamedQueryRepository implements PanacheRepository<NamedQueryEntity> {
}
