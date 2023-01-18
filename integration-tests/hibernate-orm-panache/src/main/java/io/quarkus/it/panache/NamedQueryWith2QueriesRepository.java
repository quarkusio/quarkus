package io.quarkus.it.panache;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class NamedQueryWith2QueriesRepository implements PanacheRepository<NamedQueryWith2QueriesEntity> {
}
