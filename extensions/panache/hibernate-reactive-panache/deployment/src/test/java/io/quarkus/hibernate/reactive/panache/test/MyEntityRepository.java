package io.quarkus.hibernate.reactive.panache.test;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;

@ApplicationScoped
public class MyEntityRepository implements PanacheRepository<MyEntity> {
}
