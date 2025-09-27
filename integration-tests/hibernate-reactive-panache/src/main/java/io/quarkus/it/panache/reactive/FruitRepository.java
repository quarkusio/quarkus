package io.quarkus.it.panache.reactive;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;

@ApplicationScoped
public class FruitRepository implements PanacheRepositoryBase<Fruit, Long> {
}
